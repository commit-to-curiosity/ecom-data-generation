package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.customer.config.CustomerGenerationStrategy
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class CustomerConfigLoaderTest extends AnyFunSuite {

  test("should load complete customer configuration") {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile
        .getConfig(DataGenerationConstants.CUSTOMER)

    val customerConfig =
      CustomerLoaderFactory
        .createCustomerConfigLoader()
        .loadConfiguration(config)

    assert(customerConfig.lifecycle.asOfDate == LocalDate.of(2026, 1, 1))
    assert(customerConfig.lifecycle.registrationHistoryDays == 3650)
    assert(customerConfig.demographics.ageBands.size == 6)
    assert(customerConfig.demographics.gender.male == 0.50)
    assert(customerConfig.status.active == 0.92)
    assert(customerConfig.segment.standard == 0.70)
    assert(customerConfig.acquisition.channel.organic == 0.35)
    assert(customerConfig.acquisition.campaign.social.youtube == 0.25)
    assert(customerConfig.behavior.preferredDevice.mobile == 0.70)
    assert(customerConfig.behavior.preferredPaymentMethod.upi == 0.45)
  }

  test("configured probability groups should sum to one") {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile
        .getConfig(DataGenerationConstants.CUSTOMER)

    val customerConfig =
      CustomerLoaderFactory
        .createCustomerConfigLoader()
        .loadConfiguration(config)

    assertApproximatelyOne(customerConfig.demographics.ageBands.map(_.weight).sum)
    assertApproximatelyOne(
      customerConfig.demographics.gender.male +
        customerConfig.demographics.gender.female +
        customerConfig.demographics.gender.other
    )
    assertApproximatelyOne(
      customerConfig.status.active +
        customerConfig.status.inactive +
        customerConfig.status.suspended +
        customerConfig.status.closed
    )
    assertApproximatelyOne(
      customerConfig.segment.standard +
        customerConfig.segment.premium +
        customerConfig.segment.vip +
        customerConfig.segment.business
    )
    assertApproximatelyOne(
      customerConfig.acquisition.channel.organic +
        customerConfig.acquisition.channel.paidSearch +
        customerConfig.acquisition.channel.social +
        customerConfig.acquisition.channel.email +
        customerConfig.acquisition.channel.direct +
        customerConfig.acquisition.channel.referral
    )
    assertApproximatelyOne(
      customerConfig.behavior.preferredDevice.mobile +
        customerConfig.behavior.preferredDevice.desktop +
        customerConfig.behavior.preferredDevice.tablet
    )
    assertApproximatelyOne(
      customerConfig.behavior.preferredPaymentMethod.upi +
        customerConfig.behavior.preferredPaymentMethod.creditCard +
        customerConfig.behavior.preferredPaymentMethod.debitCard +
        customerConfig.behavior.preferredPaymentMethod.netBanking +
        customerConfig.behavior.preferredPaymentMethod.wallet
    )
  }

  test("registration history days must be non-negative") {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile
        .getConfig(DataGenerationConstants.CUSTOMER)
        .withValue(
          s"${DataGenerationConstants.LIFECYCLE}.${DataGenerationConstants.REGISTRATION_HISTORY_DAYS}",
          com.typesafe.config.ConfigValueFactory.fromAnyRef(-1)
        )

    intercept[IllegalArgumentException] {
      CustomerLoaderFactory
        .createCustomerConfigLoader()
        .loadConfiguration(config)
    }
  }

  test("should default to independent strategy when generation config is absent") {
    val config = loadBaseCustomerConfig().withoutPath(
      DataGenerationConstants.GENERATION
    )

    val customerConfig = loadCustomerConfig(config)

    assert(
      customerConfig.generation.strategy ==
        CustomerGenerationStrategy.INDEPENDENT
    )
  }

  test("should load independent generation strategy") {
    val config = loadBaseCustomerConfig().withValue(
      s"${DataGenerationConstants.GENERATION}.${DataGenerationConstants.STRATEGY}",
      ConfigValueFactory.fromAnyRef("independent")
    )

    val customerConfig = loadCustomerConfig(config)

    assert(
      customerConfig.generation.strategy ==
        CustomerGenerationStrategy.INDEPENDENT
    )
  }

  test("should load correlated generation strategy") {
    val config = loadBaseCustomerConfig()
      .withoutPath(
        s"${DataGenerationConstants.GENERATION}.${DataGenerationConstants.CORRELATIONS}"
      )
      .withValue(
        s"${DataGenerationConstants.GENERATION}.${DataGenerationConstants.STRATEGY}",
        ConfigValueFactory.fromAnyRef("correlated")
      )

    intercept[IllegalArgumentException] {
      loadCustomerConfig(config)
    }
  }

  test("should default to independent strategy when strategy is absent") {
    val config = loadBaseCustomerConfig().withValue(
      DataGenerationConstants.GENERATION,
      ConfigValueFactory.fromMap(new java.util.HashMap[String, AnyRef]())
    )

    val customerConfig = loadCustomerConfig(config)

    assert(
      customerConfig.generation.strategy ==
        CustomerGenerationStrategy.INDEPENDENT
    )
  }

  test("should reject unsupported generation strategy") {
    val config = loadBaseCustomerConfig().withValue(
      s"${DataGenerationConstants.GENERATION}.${DataGenerationConstants.STRATEGY}",
      ConfigValueFactory.fromAnyRef("unknown")
    )

    intercept[IllegalArgumentException] {
      loadCustomerConfig(config)
    }
  }

  test("should load correlated generation strategy with valid correlations") {
    val correlationConfig = ConfigFactory.parseString("""
    generation {
      strategy = "correlated"

      correlations {
        segment_by_age_band = [
          { min_age = 18, max_age = 24, standard = 0.70, premium = 0.20, vip = 0.08, business = 0.02 },
          { min_age = 25, max_age = 34, standard = 0.70, premium = 0.20, vip = 0.08, business = 0.02 },
          { min_age = 35, max_age = 44, standard = 0.70, premium = 0.20, vip = 0.08, business = 0.02 },
          { min_age = 45, max_age = 54, standard = 0.70, premium = 0.20, vip = 0.08, business = 0.02 },
          { min_age = 55, max_age = 64, standard = 0.70, premium = 0.20, vip = 0.08, business = 0.02 },
          { min_age = 65, max_age = 80, standard = 0.70, premium = 0.20, vip = 0.08, business = 0.02 }
        ]

        preferred_device_by_age_band = [
          { min_age = 18, max_age = 24, mobile = 0.70, desktop = 0.25, tablet = 0.05 },
          { min_age = 25, max_age = 34, mobile = 0.70, desktop = 0.25, tablet = 0.05 },
          { min_age = 35, max_age = 44, mobile = 0.70, desktop = 0.25, tablet = 0.05 },
          { min_age = 45, max_age = 54, mobile = 0.70, desktop = 0.25, tablet = 0.05 },
          { min_age = 55, max_age = 64, mobile = 0.70, desktop = 0.25, tablet = 0.05 },
          { min_age = 65, max_age = 80, mobile = 0.70, desktop = 0.25, tablet = 0.05 }
        ]

        preferred_payment_method_by_segment = [
          { segment = "standard", upi = 0.45, credit_card = 0.20, debit_card = 0.15, net_banking = 0.10, wallet = 0.10 },
          { segment = "premium", upi = 0.45, credit_card = 0.20, debit_card = 0.15, net_banking = 0.10, wallet = 0.10 },
          { segment = "vip", upi = 0.45, credit_card = 0.20, debit_card = 0.15, net_banking = 0.10, wallet = 0.10 },
          { segment = "business", upi = 0.45, credit_card = 0.20, debit_card = 0.15, net_banking = 0.10, wallet = 0.10 }
        ]
      }
    }
  """)

    val config =
      correlationConfig.withFallback(loadBaseCustomerConfig())

    val customerConfig = loadCustomerConfig(config)

    assert(
      customerConfig.generation.strategy ==
        CustomerGenerationStrategy.CORRELATED
    )
    assert(customerConfig.generation.correlations.nonEmpty)
    assert(customerConfig.generation.correlations.get.segmentByAgeBand.size == 6)
    assert(
      customerConfig.generation.correlations.get
        .preferredDeviceByAgeBand.size == 6
    )
    assert(
      customerConfig.generation.correlations.get
        .preferredPaymentMethodBySegment.size == 4
    )
  }


  test("independent strategy should reject negative demographic ages") {
    val invalidAgeBands = ConfigFactory.parseString("""
    demographics {
      age_bands = [
        { min_age = -1, max_age = 24, weight = 0.10 },
        { min_age = 25, max_age = 34, weight = 0.25 },
        { min_age = 35, max_age = 44, weight = 0.30 },
        { min_age = 45, max_age = 54, weight = 0.20 },
        { min_age = 55, max_age = 64, weight = 0.10 },
        { min_age = 65, max_age = 80, weight = 0.05 }
      ]
    }
  """).withFallback(loadBaseCustomerConfig())

    intercept[IllegalArgumentException] {
      loadCustomerConfig(invalidAgeBands)
    }
  }

  test("independent strategy should reject gaps between demographic age bands") {
    val invalidAgeBands = ConfigFactory.parseString("""
    demographics {
      age_bands = [
        { min_age = 18, max_age = 24, weight = 0.10 },
        { min_age = 26, max_age = 34, weight = 0.25 },
        { min_age = 35, max_age = 44, weight = 0.30 },
        { min_age = 45, max_age = 54, weight = 0.20 },
        { min_age = 55, max_age = 64, weight = 0.10 },
        { min_age = 65, max_age = 80, weight = 0.05 }
      ]
    }
  """).withFallback(loadBaseCustomerConfig())

    intercept[IllegalArgumentException] {
      loadCustomerConfig(invalidAgeBands)
    }
  }


  private def loadBaseCustomerConfig(): Config = {
    ConfigReaderFactory
      .createConfigReader(ConfigFileFormat.HOCON)
      .readConfigFile
      .getConfig(DataGenerationConstants.CUSTOMER)
  }

  private def loadCustomerConfig(config: Config) = {
    CustomerLoaderFactory
      .createCustomerConfigLoader()
      .loadConfiguration(config)
  }

  private def assertApproximatelyOne(value: Double): Unit = {
    assert(math.abs(value - 1.0) < 0.000001)
  }
}
