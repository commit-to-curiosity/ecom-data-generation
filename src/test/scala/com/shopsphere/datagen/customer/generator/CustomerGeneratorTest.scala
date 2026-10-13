package com.shopsphere.datagen.customer.generator

import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.{ConfigFileFormat, CustomerAcquisitionChannel}
import com.shopsphere.datagen.customer.config.CustomerConfig
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

    assert(customer.id == "CUST000001")
    assert(customer.firstName.nonEmpty)
    assert(customer.lastName.nonEmpty)
    assert(customer.email.nonEmpty)
    assert(customer.phone.nonEmpty)
    assert(!customer.registrationDate.isAfter(customerConfig.lifecycle.asOfDate))
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
    assertCampaignMatchesChannel(customer.acquisitionChannel, customer.acquisitionCampaign)
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
        assert(Set("instagram", "facebook", "youtube").contains(acquisitionCampaign))
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
}
