
package com.shopsphere.datagen.customer.config.correlation

import com.shopsphere.datagen.common.enums.CustomerSegment
import com.shopsphere.datagen.customer.config.{
  CustomerAgeBandConfig,
  CustomerDemographicsConfigValidator,
  CustomerSegmentConfig
}

object CustomerCorrelationsConfigValidator {

  private val TOLERANCE = 0.000001

  def validate(
                correlations: CustomerCorrelationsConfig,
                ageBands: Seq[CustomerAgeBandConfig],
                segmentConfig: CustomerSegmentConfig
              ): Unit = {

    // Use the shared validator as the single source of truth for
    // demographic age ranges and population weights.
    CustomerDemographicsConfigValidator.validate(ageBands)

    val expectedRanges =
      ageBands.map(band => (band.minAge, band.maxAge))

    validateAgeBandCoverage(
      "Segment by age band",
      expectedRanges,
      correlations.segmentByAgeBand.map(
        band => (band.minAge, band.maxAge)
      )
    )

    validateAgeBandCoverage(
      "Preferred device by age band",
      expectedRanges,
      correlations.preferredDeviceByAgeBand.map(
        band => (band.minAge, band.maxAge)
      )
    )

    correlations.segmentByAgeBand.foreach { band =>
      validateProbabilityVector(
        s"Segment distribution for ages ${band.minAge}-${band.maxAge}",
        Seq(
          band.standard,
          band.premium,
          band.vip,
          band.business
        )
      )
    }

    correlations.preferredDeviceByAgeBand.foreach { band =>
      validateProbabilityVector(
        s"Device distribution for ages ${band.minAge}-${band.maxAge}",
        Seq(
          band.mobile,
          band.desktop,
          band.tablet
        )
      )
    }

    val paymentRows =
      correlations.preferredPaymentMethodBySegment

    require(
      paymentRows.map(_.segment).distinct.size == paymentRows.size,
      "Duplicate payment-method distributions found for customer segments"
    )

    require(
      paymentRows.map(_.segment).toSet ==
        CustomerSegment.allSegments.toSet,
      "Payment-method distributions must cover every customer segment"
    )

    paymentRows.foreach { row =>
      validateProbabilityVector(
        s"Payment-method distribution for segment ${row.segment.name}",
        Seq(
          row.upi,
          row.creditCard,
          row.debitCard,
          row.netBanking,
          row.wallet
        )
      )
    }

    validateAggregateSegmentProportions(
      correlations,
      ageBands,
      segmentConfig
    )
  }

  private def validateAgeBandCoverage(
                                       name: String,
                                       expected: Seq[(Int, Int)],
                                       actual: Seq[(Int, Int)]
                                     ): Unit = {

    require(
      expected.distinct.size == expected.size,
      "Demographic age bands contain duplicate ranges"
    )

    require(
      actual.distinct.size == actual.size,
      s"$name contains duplicate age ranges"
    )

    require(
      actual.toSet == expected.toSet,
      s"$name must cover exactly the configured demographic age bands"
    )
  }

  private def validateProbabilityVector(
                                         name: String,
                                         weights: Seq[Double]
                                       ): Unit = {

    require(
      weights.nonEmpty &&
        weights.forall(weight =>
          !weight.isNaN && !weight.isInfinity && weight >= 0.0
        ),
      s"$name contains invalid weights"
    )

    val total = weights.sum

    require(
      !total.isNaN &&
        !total.isInfinity &&
        math.abs(total - 1.0) <= TOLERANCE,
      s"$name weights must sum to 1.0; actual total: $total"
    )
  }

  private def validateAggregateSegmentProportions(
                                                   correlations: CustomerCorrelationsConfig,
                                                   ageBands: Seq[CustomerAgeBandConfig],
                                                   segmentConfig: CustomerSegmentConfig
                                                 ): Unit = {

    val targetWeights: Map[CustomerSegment, Double] = Map(
      CustomerSegment.STANDARD -> segmentConfig.standard,
      CustomerSegment.PREMIUM -> segmentConfig.premium,
      CustomerSegment.VIP -> segmentConfig.vip,
      CustomerSegment.BUSINESS -> segmentConfig.business
    )

    validateProbabilityVector(
      "Target customer segment",
      targetWeights.values.toSeq
    )

    val conditionalRows =
      correlations.segmentByAgeBand.map { row =>
        (row.minAge, row.maxAge) -> row
      }.toMap

    val actualWeights: Map[CustomerSegment, Double] = Map(
      CustomerSegment.STANDARD ->
        ageBands.map { ageBand =>
          ageBand.weight *
            conditionalRows(
              (ageBand.minAge, ageBand.maxAge)
            ).standard
        }.sum,

      CustomerSegment.PREMIUM ->
        ageBands.map { ageBand =>
          ageBand.weight *
            conditionalRows(
              (ageBand.minAge, ageBand.maxAge)
            ).premium
        }.sum,

      CustomerSegment.VIP ->
        ageBands.map { ageBand =>
          ageBand.weight *
            conditionalRows(
              (ageBand.minAge, ageBand.maxAge)
            ).vip
        }.sum,

      CustomerSegment.BUSINESS ->
        ageBands.map { ageBand =>
          ageBand.weight *
            conditionalRows(
              (ageBand.minAge, ageBand.maxAge)
            ).business
        }.sum
    )

    CustomerSegment.allSegments.foreach { segment =>
      val expected = targetWeights(segment)
      val actual = actualWeights(segment)

      require(
        math.abs(actual - expected) <= TOLERANCE,
        s"Aggregate proportion for ${segment.name} must be $expected, actual: $actual"
      )
    }
  }
}
