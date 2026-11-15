
package com.shopsphere.datagen.common.analysis

import com.fasterxml.jackson.databind.JsonNode
import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig

import java.nio.file.Path
import java.time.{LocalDate, Period}
import scala.collection.mutable

private[analysis] final class CustomerProfileAccumulator(
                                                          asOfDate: LocalDate,
                                                          ageBands: Seq[CustomerAgeBandConfig]
                                                        ) {

  require(asOfDate != null, "As-of date must not be null")
  require(ageBands != null && ageBands.nonEmpty,
    "Age bands must not be empty")

  private val orderedBands = ageBands.toVector

  orderedBands.foreach { band =>
    require(band != null, "Age band must not be null")
    require(band.minAge >= 0 && band.maxAge >= band.minAge,
      s"Invalid age band: ${band.minAge}-${band.maxAge}")
  }

  private val sortedBands =
    orderedBands.sortBy(_.minAge)

  sortedBands.sliding(2).foreach {
    case Seq(left, right) =>
      require(left.maxAge < right.minAge,
        s"Overlapping age bands: ${left.minAge}-${left.maxAge} and ${right.minAge}-${right.maxAge}")
    case _ =>
  }

  private val genderValues =
    Vector("female", "male", "other")

  private val statusValues =
    Vector("active", "inactive", "suspended", "closed")

  private val segmentValues =
    Vector("standard", "premium", "vip", "business")

  private val channelCampaigns =
    Vector(
      "organic" -> Vector("seo", "content"),
      "paid_search" -> Vector("brand", "generic"),
      "social" -> Vector("instagram", "facebook", "youtube"),
      "email" -> Vector("newsletter", "promotion"),
      "direct" -> Vector("none"),
      "referral" -> Vector("customer_referral")
    )

  private val deviceValues =
    Vector("mobile", "desktop", "tablet")

  private val paymentMethodValues =
    Vector("upi", "credit_card", "debit_card", "net_banking", "wallet")

  private def counters(values: Seq[String]): mutable.Map[String, Long] =
    mutable.Map.from(values.map(_ -> 0L))

  private def safeTotal(counts: Iterable[Long]): Long =
    counts.foldLeft(0L) { (total, count) =>
      Math.addExact(total, count)
    }

  private val genderCounts = counters(genderValues)
  private val statusCounts = counters(statusValues)
  private val segmentCounts = counters(segmentValues)
  private val channelCounts =
    counters(channelCampaigns.map(_._1))

  private val campaignCounts =
    channelCampaigns.map { case (channel, campaigns) =>
      channel -> counters(campaigns)
    }.toMap

  private val deviceCounts = counters(deviceValues)
  private val paymentMethodCounts = counters(paymentMethodValues)

  private val ageCounts = mutable.Map.empty[Int, Long]
  private val ageBandCounts = Array.fill[Long](orderedBands.size)(0L)

  private var customerCount = 0L
  private var ageSum = 0L
  private var minAge: Option[Int] = None
  private var maxAge: Option[Int] = None

  def add(
           node: JsonNode,
           path: Path,
           lineNumber: Long
         ): Unit = {

    def requiredText(field: String): String = {
      val value = node.get(field)

      require(
        value != null && value.isTextual && value.asText().nonEmpty,
        s"Missing or invalid '$field' in $path at line $lineNumber"
      )

      value.asText()
    }

    def increment(
                   counts: mutable.Map[String, Long],
                   field: String
                 ): String = {
      val value = requiredText(field)

      require(
        counts.contains(value),
        s"Unknown $field '$value' in $path at line $lineNumber"
      )

      counts.update(value, Math.addExact(counts(value), 1L))
      value
    }

    val gender = increment(genderCounts, "gender")
    val status = increment(statusCounts, "customer_status")
    val segment = increment(segmentCounts, "customer_segment")
    val channel = increment(channelCounts, "acquisition_channel")

    val campaign = requiredText("acquisition_campaign")
    val channelCampaignCounter = campaignCounts(channel)

    require(
      channelCampaignCounter.contains(campaign),
      s"Invalid campaign '$campaign' for channel '$channel' in $path at line $lineNumber"
    )

    channelCampaignCounter.update(
      campaign,
      Math.addExact(channelCampaignCounter(campaign), 1L)
    )

    val device = increment(deviceCounts, "preferred_device")
    val paymentMethod =
      increment(paymentMethodCounts, "preferred_payment_method")

    val birthDateText = requiredText("date_of_birth")

    val birthDate =
      try {
        LocalDate.parse(birthDateText)
      } catch {
        case error: Exception =>
          throw new IllegalArgumentException(
            s"Invalid date_of_birth in $path at line $lineNumber",
            error
          )
      }

    require(
      !birthDate.isAfter(asOfDate),
      s"date_of_birth is after asOfDate in $path at line $lineNumber"
    )

    val age = Period.between(birthDate, asOfDate).getYears

    require(
      age >= 0,
      s"Invalid customer age in $path at line $lineNumber"
    )

    val bandIndex =
      orderedBands.indexWhere(
        band => age >= band.minAge && age <= band.maxAge
      )

    require(
      bandIndex >= 0,
      s"Age $age does not belong to a configured age band in $path at line $lineNumber"
    )

    customerCount = Math.addExact(customerCount, 1L)
    ageSum = Math.addExact(ageSum, age.toLong)

    ageCounts.update(
      age,
      Math.addExact(ageCounts.getOrElse(age, 0L), 1L)
    )

    ageBandCounts(bandIndex) =
      Math.addExact(ageBandCounts(bandIndex), 1L)

    minAge = Some(minAge.fold(age)(math.min(_, age)))
    maxAge = Some(maxAge.fold(age)(math.max(_, age)))
  }

  def result(): CustomerProfileSummary = {

    def distribution(
                      counts: mutable.Map[String, Long]
                    ): CategoryDistribution =
      CategoryDistribution(
        total = safeTotal(counts.values),
        categories = counts.toVector
          .sortBy(_._1)
          .map { case (value, count) =>
            CategoryFrequency(
              value,
              count,
              if (customerCount == 0L) 0.0
              else count.toDouble / customerCount
            )
          }
      )

    def campaignDistribution(
                              channel: String,
                              counts: mutable.Map[String, Long]
                            ): ChannelCampaignDistribution = {
      val total = safeTotal(counts.values)

      ChannelCampaignDistribution(
        channel = channel,
        totalCustomers = total,
        campaigns = channelCampaigns
          .find(_._1 == channel)
          .get._2
          .map { campaign =>
            val count = counts(campaign)

            CategoryFrequency(
              campaign,
              count,
              if (total == 0L) 0.0
              else count.toDouble / total
            )
          }
      )
    }

    def medianAge: Option[Double] = {
      if (customerCount == 0L) {
        None
      } else {
        val positions =
          Set((customerCount - 1L) / 2L, customerCount / 2L)

        var cumulative = 0L
        val values = mutable.Map.empty[Long, Int]

        ageCounts.toVector.sortBy(_._1).foreach {
          case (age, count) =>
            val previous = cumulative
            cumulative = Math.addExact(cumulative, count)

            positions.foreach { position =>
              if (position >= previous && position < cumulative) {
                values.update(position, age)
              }
            }
        }

        Some(
          (values((customerCount - 1L) / 2L).toDouble +
            values(customerCount / 2L).toDouble) / 2.0
        )
      }
    }

    CustomerProfileSummary(
      customerCount = customerCount,
      age = AgeStatistics(
        minAge = minAge,
        maxAge = maxAge,
        meanAge =
          if (customerCount == 0L) None
          else Some(ageSum.toDouble / customerCount),
        medianAge = medianAge,
        ageBands = orderedBands.zipWithIndex.map {
          case (band, index) =>
            val count = ageBandCounts(index)

            AgeBandFrequency(
              band.minAge,
              band.maxAge,
              count,
              if (customerCount == 0L) 0.0
              else count.toDouble / customerCount
            )
        }
      ),
      gender = distribution(genderCounts),
      customerStatus = distribution(statusCounts),
      customerSegment = distribution(segmentCounts),
      acquisitionChannel = distribution(channelCounts),
      acquisitionCampaignByChannel =
        channelCampaigns.map { case (channel, _) =>
          campaignDistribution(channel, campaignCounts(channel))
        },
      preferredDevice = distribution(deviceCounts),
      preferredPaymentMethod = distribution(paymentMethodCounts)
    )
  }
}
