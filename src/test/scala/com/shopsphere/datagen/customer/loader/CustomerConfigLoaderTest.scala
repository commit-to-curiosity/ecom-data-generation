package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
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

  private def assertApproximatelyOne(value: Double): Unit = {
    assert(math.abs(value - 1.0) < 0.000001)
  }
}
