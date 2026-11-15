
package com.shopsphere.datagen.customer.generator

import com.shopsphere.datagen.common.config.{
  ConfigReaderFactory,
  DataGenerationConstants
}
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.{
  ConfigFileFormat,
  CustomerAcquisitionChannel,
  CustomerSegment
}
import com.shopsphere.datagen.customer.config.{
  CustomerConfig,
  CustomerGenerationConfig,
  CustomerGenerationStrategy
}
import com.shopsphere.datagen.customer.config.correlation.{
  AgeBandDeviceDistributionConfig,
  AgeBandSegmentDistributionConfig,
  CustomerCorrelationsConfig,
  CustomerCorrelationsConfigValidator,
  SegmentPaymentMethodDistributionConfig
}
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import org.scalatest.funsuite.AnyFunSuite

import java.time.temporal.ChronoUnit

class CustomerGeneratorTest extends AnyFunSuite {

  test("should generate a valid customer") {
    val customerConfig = loadCustomerConfig()
    val randomGenerator = new RandomGenerator(42L)

    val generator =
      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      )

    val customer = generator.generate()

    assert(customer.firstName == "Lamar")
    assert(customer.lastName == "Lehner")
    assert(customer.email == "lamar.lehner@hotmail.com")
    assert(customer.phone == "(305) 489-3226")
    assert(customer.gender == "male")
    assert(customer.dateOfBirth.toString == "1974-01-01")
    assert(customer.registrationDate.toString == "2017-07-11")
    assert(customer.customerStatus == "active")
    assert(customer.customerSegment == "vip")
    assert(customer.acquisitionChannel == "email")
    assert(customer.acquisitionCampaign == "newsletter")
    assert(customer.preferredDevice == "mobile")
    assert(customer.preferredPaymentMethod == "upi")

    assert(customer.id == "CUST000001")
    assert(customer.firstName.nonEmpty)
    assert(customer.lastName.nonEmpty)
    assert(customer.email.nonEmpty)
    assert(customer.phone.nonEmpty)

    assert(
      !customer.registrationDate.isAfter(
        customerConfig.lifecycle.asOfDate
      )
    )

    assert(
      !customer.registrationDate.isBefore(
        customerConfig.lifecycle.asOfDate.minusDays(
          customerConfig.lifecycle.registrationHistoryDays.toLong
        )
      )
    )

    val age =
      ChronoUnit.YEARS.between(
        customer.dateOfBirth,
        customerConfig.lifecycle.asOfDate
      )

    assert(age >= 18)
    assert(age <= 80)

    assertCampaignMatchesChannel(
      customer.acquisitionChannel,
      customer.acquisitionCampaign
    )
  }

  test("should generate deterministic customers for the same seed") {
    val customerConfig = loadCustomerConfig()

    def generateCustomers(seed: Long) = {
      val randomGenerator = new RandomGenerator(seed)

      val generator =
        new CustomerGenerator(
          customerConfig,
          randomGenerator,
          new CustomerIdentityGenerator(randomGenerator)
        )

      (1 to 5).map(_ => generator.generate())
    }

    assert(generateCustomers(42L) == generateCustomers(42L))
  }

  test("should generate different customers for different seeds") {
    val customerConfig = loadCustomerConfig()

    def generateCustomer(seed: Long) = {
      val randomGenerator = new RandomGenerator(seed)

      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      ).generate()
    }

    assert(generateCustomer(42L) != generateCustomer(43L))
  }

  test("correlated mode should apply conditional distributions") {
    val customerConfig = loadCorrelatedCustomerConfig()
    val randomGenerator = new RandomGenerator(42L)

    val generator =
      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      )

    val customers = (1 to 5000).map(_ => generator.generate())

    def customerAge(
                     customer: com.shopsphere.datagen.customer.model.Customer
                   ): Int =
      ChronoUnit.YEARS.between(
        customer.dateOfBirth,
        customerConfig.lifecycle.asOfDate
      ).toInt

    val youngCustomers =
      customers.filter(customerAge(_) <= 24)

    assert(youngCustomers.nonEmpty)

    assert(
      youngCustomers.forall(_.customerSegment == "standard")
    )

    assert(
      youngCustomers.forall(_.preferredDevice == "desktop")
    )

    val otherCustomers =
      customers.filter(customerAge(_) > 24)

    assert(
      otherCustomers.forall(_.preferredDevice == "mobile")
    )

    val expectedPaymentMethods = Map(
      "standard" -> "upi",
      "premium" -> "credit_card",
      "vip" -> "debit_card",
      "business" -> "wallet"
    )

    customers.foreach { customer =>
      assert(
        customer.preferredPaymentMethod ==
          expectedPaymentMethods(customer.customerSegment)
      )
    }
  }

  test(
    "correlated mode should generate deterministic customers for the same seed"
  ) {
    val customerConfig = loadCorrelatedCustomerConfig()

    def generateCustomers(seed: Long) = {
      val randomGenerator = new RandomGenerator(seed)

      val generator =
        new CustomerGenerator(
          customerConfig,
          randomGenerator,
          new CustomerIdentityGenerator(randomGenerator)
        )

      (1 to 100).map(_ => generator.generate())
    }

    assert(generateCustomers(42L) == generateCustomers(42L))
  }

  test(
    "correlated mode should reproduce configured distributions over 100000 customers"
  ) {
    val customerConfig = loadCorrelatedCustomerConfig()
    val randomGenerator = new RandomGenerator(42L)

    val generator =
      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      )

    val customers =
      (1 to 100000).map(_ => generator.generate())

    def customerAge(
                     customer: com.shopsphere.datagen.customer.model.Customer
                   ): Int =
      ChronoUnit.YEARS.between(
        customer.dateOfBirth,
        customerConfig.lifecycle.asOfDate
      ).toInt

    def ageBandOf(
                   customer: com.shopsphere.datagen.customer.model.Customer
                 ): (Int, Int) = {
      val age = customerAge(customer)

      customerConfig.demographics.ageBands
        .find(band => age >= band.minAge && age <= band.maxAge)
        .map(band => band.minAge -> band.maxAge)
        .getOrElse {
          fail(
            s"Generated customer age $age does not belong to a configured age band"
          )
        }
    }

    def assertProportion(
                          actualCount: Int,
                          totalCount: Int,
                          expected: Double,
                          tolerance: Double,
                          description: String
                        ): Unit = {
      assert(totalCount > 0, s"No customers available for $description")

      val actual = actualCount.toDouble / totalCount

      assert(
        math.abs(actual - expected) <= tolerance,
        s"$description: expected proportion $expected, " +
          s"actual proportion $actual, tolerance $tolerance"
      )
    }

    // 1. Verify age-band proportions.
    val customersByAgeBand =
      customers.groupBy(ageBandOf)

    customerConfig.demographics.ageBands.foreach { band =>
      val key = band.minAge -> band.maxAge

      val bandCustomers =
        customersByAgeBand.getOrElse(key, Seq.empty)

      assertProportion(
        actualCount = bandCustomers.size,
        totalCount = customers.size,
        expected = band.weight,
        tolerance = 0.005,
        description = s"Age band ${band.minAge}-${band.maxAge}"
      )
    }

    // 2. Verify overall customer-segment proportions.
    val expectedSegmentProportions = Map(
      "standard" -> customerConfig.segment.standard,
      "premium" -> customerConfig.segment.premium,
      "vip" -> customerConfig.segment.vip,
      "business" -> customerConfig.segment.business
    )

    expectedSegmentProportions.foreach {
      case (segment, expected) =>
        val actualCount =
          customers.count(_.customerSegment == segment)

        assertProportion(
          actualCount = actualCount,
          totalCount = customers.size,
          expected = expected,
          tolerance = 0.005,
          description = s"Overall segment $segment"
        )
    }

    // 3. Verify segment proportions within each age band.
    customersByAgeBand.foreach {
      case ((minAge, maxAge), bandCustomers) =>
        val expectedSegments =
          if (minAge == 18 && maxAge == 24) {
            Map(
              "standard" -> 1.0,
              "premium" -> 0.0,
              "vip" -> 0.0,
              "business" -> 0.0
            )
          } else {
            Map(
              "standard" -> (2.0 / 3.0),
              "premium" -> (2.0 / 9.0),
              "vip" -> (4.0 / 45.0),
              "business" -> (1.0 / 45.0)
            )
          }

        expectedSegments.foreach {
          case (segment, expected) =>
            val actualCount =
              bandCustomers.count(_.customerSegment == segment)

            assertProportion(
              actualCount = actualCount,
              totalCount = bandCustomers.size,
              expected = expected,
              tolerance = 0.03,
              description =
                s"Segment $segment in age band $minAge-$maxAge"
            )
        }
    }

    // 4. Verify preferred device distributions by age band.
    customersByAgeBand.foreach {
      case ((minAge, maxAge), bandCustomers) =>
        val expectedDevice =
          if (minAge == 18 && maxAge == 24) "desktop"
          else "mobile"

        val actualCount =
          bandCustomers.count(_.preferredDevice == expectedDevice)

        assertProportion(
          actualCount = actualCount,
          totalCount = bandCustomers.size,
          expected = 1.0,
          tolerance = 0.0,
          description =
            s"Preferred device $expectedDevice in age band $minAge-$maxAge"
        )
    }

    // 5. Verify preferred payment method distributions by segment.
    val expectedPaymentMethods = Map(
      "standard" -> "upi",
      "premium" -> "credit_card",
      "vip" -> "debit_card",
      "business" -> "wallet"
    )

    val customersBySegment =
      customers.groupBy(_.customerSegment)

    expectedPaymentMethods.foreach {
      case (segment, expectedPaymentMethod) =>
        val segmentCustomers =
          customersBySegment.getOrElse(segment, Seq.empty)

        assert(
          segmentCustomers.nonEmpty,
          s"No customers generated for $segment"
        )

        val actualCount =
          segmentCustomers.count(
            _.preferredPaymentMethod == expectedPaymentMethod
          )

        assertProportion(
          actualCount = actualCount,
          totalCount = segmentCustomers.size,
          expected = 1.0,
          tolerance = 0.0,
          description =
            s"Payment method $expectedPaymentMethod for segment $segment"
        )
    }
  }

  test("should generate customer with an explicit global ordinal") {
    val customerConfig = loadCustomerConfig()
    val randomGenerator = new RandomGenerator(42L)

    val generator =
      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      )

    assert(generator.generate(10001L).id == "CUST010001")
  }

  test("explicit customer ordinals should not depend on call order") {
    val customerConfig = loadCustomerConfig()

    def createGenerator(seed: Long) = {
      val randomGenerator = new RandomGenerator(seed)

      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      )
    }

    val first = createGenerator(42L)
    val second = createGenerator(42L)

    assert(first.generate(2L).id == "CUST000002")
    assert(first.generate(10001L).id == "CUST010001")

    assert(second.generate(10001L).id == "CUST010001")
    assert(second.generate(2L).id == "CUST000002")
  }

  test("explicit customer generation should reject non-positive ordinals") {
    val customerConfig = loadCustomerConfig()
    val randomGenerator = new RandomGenerator(42L)

    val generator =
      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        new CustomerIdentityGenerator(randomGenerator)
      )

    assertThrows[IllegalArgumentException] {
      generator.generate(0L)
    }
  }

  private def loadCustomerConfig(): CustomerConfig = {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile
        .getConfig(DataGenerationConstants.CUSTOMER)

    CustomerLoaderFactory
      .createCustomerConfigLoader()
      .loadConfiguration(config)
  }

  private def assertCampaignMatchesChannel(
                                            acquisitionChannel: String,
                                            acquisitionCampaign: String
                                          ): Unit = {
    acquisitionChannel match {
      case channel if channel == CustomerAcquisitionChannel.ORGANIC.name =>
        assert(Set("seo", "content").contains(acquisitionCampaign))

      case channel if channel == CustomerAcquisitionChannel.PAID_SEARCH.name =>
        assert(Set("brand", "generic").contains(acquisitionCampaign))

      case channel if channel == CustomerAcquisitionChannel.SOCIAL.name =>
        assert(
          Set("instagram", "facebook", "youtube").contains(acquisitionCampaign)
        )

      case channel if channel == CustomerAcquisitionChannel.EMAIL.name =>
        assert(Set("newsletter", "promotion").contains(acquisitionCampaign))

      case channel if channel == CustomerAcquisitionChannel.DIRECT.name =>
        assert(acquisitionCampaign == "none")

      case channel if channel == CustomerAcquisitionChannel.REFERRAL.name =>
        assert(acquisitionCampaign == "customer_referral")

      case other =>
        fail(s"Unexpected acquisition channel: $other")
    }
  }

  private def loadCorrelatedCustomerConfig(): CustomerConfig = {
    val baseConfig = loadCustomerConfig()

    val segmentDistributions =
      baseConfig.demographics.ageBands.map { band =>
        if (band.minAge == 18 && band.maxAge == 24) {
          AgeBandSegmentDistributionConfig(
            minAge = 18,
            maxAge = 24,
            standard = 1.0,
            premium = 0.0,
            vip = 0.0,
            business = 0.0
          )
        } else {
          AgeBandSegmentDistributionConfig(
            minAge = band.minAge,
            maxAge = band.maxAge,
            standard = 2.0 / 3.0,
            premium = 2.0 / 9.0,
            vip = 4.0 / 45.0,
            business = 1.0 / 45.0
          )
        }
      }

    val deviceDistributions =
      baseConfig.demographics.ageBands.map { band =>
        if (band.minAge == 18 && band.maxAge == 24) {
          AgeBandDeviceDistributionConfig(
            band.minAge,
            band.maxAge,
            mobile = 0.0,
            desktop = 1.0,
            tablet = 0.0
          )
        } else {
          AgeBandDeviceDistributionConfig(
            band.minAge,
            band.maxAge,
            mobile = 1.0,
            desktop = 0.0,
            tablet = 0.0
          )
        }
      }

    val paymentDistributions =
      Seq(
        SegmentPaymentMethodDistributionConfig(
          CustomerSegment.STANDARD,
          upi = 1.0,
          creditCard = 0.0,
          debitCard = 0.0,
          netBanking = 0.0,
          wallet = 0.0
        ),
        SegmentPaymentMethodDistributionConfig(
          CustomerSegment.PREMIUM,
          upi = 0.0,
          creditCard = 1.0,
          debitCard = 0.0,
          netBanking = 0.0,
          wallet = 0.0
        ),
        SegmentPaymentMethodDistributionConfig(
          CustomerSegment.VIP,
          upi = 0.0,
          creditCard = 0.0,
          debitCard = 1.0,
          netBanking = 0.0,
          wallet = 0.0
        ),
        SegmentPaymentMethodDistributionConfig(
          CustomerSegment.BUSINESS,
          upi = 0.0,
          creditCard = 0.0,
          debitCard = 0.0,
          netBanking = 0.0,
          wallet = 1.0
        )
      )

    val correlations = CustomerCorrelationsConfig(
      segmentByAgeBand = segmentDistributions,
      preferredDeviceByAgeBand = deviceDistributions,
      preferredPaymentMethodBySegment = paymentDistributions
    )

    CustomerCorrelationsConfigValidator.validate(
      correlations,
      baseConfig.demographics.ageBands,
      baseConfig.segment
    )

    baseConfig.copy(
      generation = CustomerGenerationConfig(
        strategy = CustomerGenerationStrategy.CORRELATED,
        correlations = Some(correlations)
      )
    )
  }
}
