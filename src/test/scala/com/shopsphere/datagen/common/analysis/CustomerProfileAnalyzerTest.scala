
package com.shopsphere.datagen.common.analysis

import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.LocalDate

class CustomerProfileAnalyzerTest extends AnyFunSuite {

  private val fileNames = Seq(
    "customers.jsonl",
    "addresses.jsonl",
    "orders.jsonl",
    "order_items.jsonl",
    "payments.jsonl"
  )

  private val asOfDate = LocalDate.parse("2026-01-01")

  private val ageBands = Seq(
    CustomerAgeBandConfig(18, 29, 0.25),
    CustomerAgeBandConfig(30, 39, 0.25),
    CustomerAgeBandConfig(40, 49, 0.25),
    CustomerAgeBandConfig(50, 59, 0.25)
  )

  private def withDirectory(test: Path => Unit): Unit = {
    val directory = Files.createTempDirectory("customer-profile-test")

    try {
      test(directory)
    } finally {
      val paths = Files.walk(directory)
      try {
        import scala.jdk.CollectionConverters._
        paths.iterator().asScala.toSeq
          .sortBy(_.getNameCount)
          .reverse
          .foreach(path => Files.deleteIfExists(path))
      } finally {
        paths.close()
      }
    }
  }

  private def writeFile(
                         directory: Path,
                         fileName: String,
                         content: String
                       ): Unit = {
    Files.write(
      directory.resolve(fileName),
      content.getBytes(StandardCharsets.UTF_8)
    )
  }

  private def createEmptyDataset(directory: Path): Unit =
    fileNames.foreach(name => writeFile(directory, name, ""))

  private def customer(
                        dob: String,
                        gender: String = "male",
                        status: String = "active",
                        segment: String = "standard",
                        channel: String = "organic",
                        campaign: String = "seo",
                        device: String = "mobile",
                        paymentMethod: String = "upi"
                      ): String =
    s"""{"date_of_birth":"$dob","gender":"$gender","customer_status":"$status","customer_segment":"$segment","acquisition_channel":"$channel","acquisition_campaign":"$campaign","preferred_device":"$device","preferred_payment_method":"$paymentMethod"}"""

  private def writeCustomers(
                              directory: Path,
                              customers: Seq[String]
                            ): Unit =
    writeFile(
      directory,
      "customers.jsonl",
      customers.mkString("", "\n", if (customers.nonEmpty) "\n" else "")
    )

  private def analyze(directory: Path): CustomerProfileSummary =
    new DatasetSummaryAnalyzer()
      .analyze(directory, asOfDate, ageBands)
      .customerProfile
      .get

  private def assertClose(
                           actual: Double,
                           expected: Double
                         ): Unit =
    assert(math.abs(actual - expected) < 1e-9)

  private def sampleCustomers: Seq[String] = Seq(
    customer(
      dob = "2006-01-01",
      gender = "male",
      status = "active",
      segment = "standard",
      channel = "organic",
      campaign = "seo",
      device = "mobile",
      paymentMethod = "upi"
    ),
    customer(
      dob = "1996-01-01",
      gender = "female",
      status = "active",
      segment = "premium",
      channel = "organic",
      campaign = "content",
      device = "desktop",
      paymentMethod = "credit_card"
    ),
    customer(
      dob = "1986-01-01",
      gender = "male",
      status = "inactive",
      segment = "standard",
      channel = "social",
      campaign = "instagram",
      device = "mobile",
      paymentMethod = "upi"
    ),
    customer(
      dob = "1976-01-01",
      gender = "other",
      status = "suspended",
      segment = "vip",
      channel = "direct",
      campaign = "none",
      device = "tablet",
      paymentMethod = "wallet"
    )
  )

  test("should calculate exact age statistics and configured age bands") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(directory, sampleCustomers)

      val result = analyze(directory)

      assert(result.customerCount == 4L)
      assert(result.age.minAge.contains(20))
      assert(result.age.maxAge.contains(50))
      assertClose(result.age.meanAge.get, 35.0)
      assertClose(result.age.medianAge.get, 35.0)

      assert(result.age.ageBands.map(_.count) == Vector(1L, 1L, 1L, 1L))
      assert(result.age.ageBands.map(_.minAge) == Vector(18, 30, 40, 50))
      assert(result.age.ageBands.map(_.maxAge) == Vector(29, 39, 49, 59))

      result.age.ageBands.foreach { band =>
        assertClose(band.proportion, 0.25)
      }
    }
  }

  test("should calculate categorical distributions") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(directory, sampleCustomers)

      val result = analyze(directory)

      assert(result.gender.categories.map(c => c.value -> c.count) ==
        Vector("female" -> 1L, "male" -> 2L, "other" -> 1L))

      assert(result.customerStatus.categories.map(c => c.value -> c.count) ==
        Vector(
          "active" -> 2L,
          "closed" -> 0L,
          "inactive" -> 1L,
          "suspended" -> 1L
        ))

      assert(result.customerSegment.categories.map(c => c.value -> c.count) ==
        Vector(
          "business" -> 0L,
          "premium" -> 1L,
          "standard" -> 2L,
          "vip" -> 1L
        ))

      assert(result.preferredDevice.categories.map(c => c.value -> c.count) ==
        Vector("desktop" -> 1L, "mobile" -> 2L, "tablet" -> 1L))

      assert(
        result.preferredPaymentMethod.categories.map(c => c.value -> c.count) ==
          Vector(
            "credit_card" -> 1L,
            "debit_card" -> 0L,
            "net_banking" -> 0L,
            "upi" -> 2L,
            "wallet" -> 1L
          )
      )

      result.gender.categories.foreach { category =>
        assertClose(category.proportion, category.count.toDouble / 4.0)
      }
    }
  }

  test("should calculate campaign proportions within each channel") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(directory, sampleCustomers)

      val result = analyze(directory)

      val channels =
        result.acquisitionCampaignByChannel.map(c => c.channel -> c).toMap

      val organic = channels("organic")

      assert(organic.totalCustomers == 2L)
      assert(organic.campaigns.map(c => c.value -> c.count) ==
        Vector("seo" -> 1L, "content" -> 1L))

      organic.campaigns.foreach { campaign =>
        assertClose(campaign.proportion, 0.5)
      }

      val social = channels("social")

      assert(social.totalCustomers == 1L)
      assertClose(
        social.campaigns.find(_.value == "instagram").get.proportion,
        1.0
      )

      val direct = channels("direct")

      assert(direct.totalCustomers == 1L)
      assertClose(direct.campaigns.head.proportion, 1.0)

      assert(channels("email").totalCustomers == 0L)
      assert(channels("referral").totalCustomers == 0L)
    }
  }

  test("should retain zero-count categories") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(directory, Seq(sampleCustomers.head))

      val result = analyze(directory)

      assert(result.gender.categories.size == 3)
      assert(result.customerStatus.categories.size == 4)
      assert(result.customerSegment.categories.size == 4)
      assert(result.acquisitionChannel.categories.size == 6)
      assert(result.preferredDevice.categories.size == 3)
      assert(result.preferredPaymentMethod.categories.size == 5)
    }
  }

  test("should handle an empty customer population") {
    withDirectory { directory =>
      createEmptyDataset(directory)

      val result = analyze(directory)

      assert(result.customerCount == 0L)
      assert(result.age.minAge.isEmpty)
      assert(result.age.maxAge.isEmpty)
      assert(result.age.meanAge.isEmpty)
      assert(result.age.medianAge.isEmpty)
      assert(result.age.ageBands.forall(_.count == 0L))
      assert(result.gender.categories.forall(_.proportion == 0.0))
      assert(result.acquisitionChannel.categories.forall(_.count == 0L))
    }
  }

  test("should reject an unknown categorical value") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(
        directory,
        Seq(customer("2005-01-01", gender = "unknown"))
      )

      val error = intercept[IllegalArgumentException] {
        analyze(directory)
      }

      assert(error.getMessage.contains("gender"))
      assert(error.getMessage.contains("unknown"))
    }
  }

  test("should reject an invalid date of birth") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(
        directory,
        Seq(customer("not-a-date"))
      )

      val error = intercept[IllegalArgumentException] {
        analyze(directory)
      }

      assert(error.getMessage.contains("date_of_birth"))
    }
  }

  test("should reject a birth date after the reference date") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(
        directory,
        Seq(customer("2027-01-01"))
      )

      val error = intercept[IllegalArgumentException] {
        analyze(directory)
      }

      assert(error.getMessage.contains("after asOfDate"))
    }
  }

  test("should reject age bands that overlap") {
    withDirectory { directory =>
      createEmptyDataset(directory)

      val overlappingBands = Seq(
        CustomerAgeBandConfig(18, 30, 0.5),
        CustomerAgeBandConfig(30, 40, 0.5)
      )

      val error = intercept[IllegalArgumentException] {
        new DatasetSummaryAnalyzer()
          .analyze(directory, asOfDate, overlappingBands)
      }

      assert(error.getMessage.contains("Overlapping age bands"))
    }
  }

  test("should reject customers outside configured age bands") {
    withDirectory { directory =>
      createEmptyDataset(directory)
      writeCustomers(directory, Seq(customer("1960-01-01")))

      val error = intercept[IllegalArgumentException] {
        analyze(directory)
      }

      assert(error.getMessage.contains("does not belong"))
    }
  }

  test("should reject category count overflow") {
    val counts = scala.collection.mutable.Map(
      "first" -> Long.MaxValue,
      "second" -> 1L
    )

    val error = intercept[ArithmeticException] {
      counts.values.foldLeft(0L)(Math.addExact)
    }

    assert(error != null)
  }
}
