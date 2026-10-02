package com.shopsphere.datagen

import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory

object Main {

  def main(args: Array[String]): Unit = {
    println("ShopSphere E-Commerce Data Generator")

    val configFormat = ConfigFileFormat.HOCON
    println(s"Configuration format: ${configFormat.name}")

    val configReader = ConfigReaderFactory.createConfigReader(configFormat)
    val config = configReader.readConfigFile

    val customerConfigLoader = CustomerLoaderFactory.createCustomerConfigLoader()
    val customerConfig: CustomerConfig = customerConfigLoader.loadConfiguration(
      config.getConfig(DataGenerationConstants.CUSTOMER)
    )

    println(s"Male probability: ${customerConfig.demographics.gender.male}")
    println(s"Female probability: ${customerConfig.demographics.gender.female}")
    println(s"Other probability: ${customerConfig.demographics.gender.other}")
    println(s"Age bands: ${customerConfig.demographics.ageBands.size}")
    println(s"Lifecycle stages: ${customerConfig.lifecycle.asOfDate}")
    println(s"Registration history days: ${customerConfig.lifecycle.registrationHistoryDays}")
    println(s"Active probability: ${customerConfig.status.active}")
    println(s"Inactive probability: ${customerConfig.status.inactive}")
    println(s"Suspended probability: ${customerConfig.status.suspended}")
    println(s"Closed probability: ${customerConfig.status.closed}")
    println(s"Standard segment probability: ${customerConfig.segment.standard}")
    println(s"Premium segment probability: ${customerConfig.segment.premium}")
    println(s"VIP segment probability: ${customerConfig.segment.vip}")
    println(s"Business segment probability: ${customerConfig.segment.business}")
    println(s"Organic acquisition probability: ${customerConfig.acquisition.channel.organic}")
    println(s"Paid search acquisition probability: ${customerConfig.acquisition.channel.paidSearch}")
    println(s"Social acquisition probability: ${customerConfig.acquisition.channel.social}")
    println(s"Email acquisition probability: ${customerConfig.acquisition.channel.email}")
    println(s"Direct acquisition probability: ${customerConfig.acquisition.channel.direct}")
    println(s"Referral acquisition probability: ${customerConfig.acquisition.channel.referral}")
    println(s"Organic SEO probability: ${customerConfig.acquisition.campaign.organic.seo}")
    println(s"Organic content probability: ${customerConfig.acquisition.campaign.organic.content}")
    println(s"Paid search brand probability: ${customerConfig.acquisition.campaign.paidSearch.brand}")
    println(s"Paid search generic probability: ${customerConfig.acquisition.campaign.paidSearch.generic}")
    println(s"Social Instagram probability: ${customerConfig.acquisition.campaign.social.instagram}")
    println(s"Social Facebook probability: ${customerConfig.acquisition.campaign.social.facebook}")
    println(s"Social YouTube probability: ${customerConfig.acquisition.campaign.social.youtube}")
    println(s"Email newsletter probability: ${customerConfig.acquisition.campaign.email.newsletter}")
    println(s"Email promotion probability: ${customerConfig.acquisition.campaign.email.promotion}")
    println(s"Direct none probability: ${customerConfig.acquisition.campaign.direct.none}")
    println(s"Referral customer probability: ${customerConfig.acquisition.campaign.referral.customerReferral}")
    println(s"Mobile preferred device probability: ${customerConfig.behavior.preferredDevice.mobile}")
    println(s"Desktop preferred device probability: ${customerConfig.behavior.preferredDevice.desktop}")
    println(s"Tablet preferred device probability: ${customerConfig.behavior.preferredDevice.tablet}")
    println(s"UPI preferred payment probability: ${customerConfig.behavior.preferredPaymentMethod.upi}")
    println(s"Credit card preferred payment probability: ${customerConfig.behavior.preferredPaymentMethod.creditCard}")
    println(s"Debit card preferred payment probability: ${customerConfig.behavior.preferredPaymentMethod.debitCard}")
    println(s"Net banking preferred payment probability: ${customerConfig.behavior.preferredPaymentMethod.netBanking}")
    println(s"Wallet preferred payment probability: ${customerConfig.behavior.preferredPaymentMethod.wallet}")
  }
}