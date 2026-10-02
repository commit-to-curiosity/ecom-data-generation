package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.CustomerDemographicsConfig
import com.typesafe.config.Config

class CustomerDemographicsLoader(
                                  genderLoader: CustomerGenderLoader,
                                  ageBandLoader: CustomerAgeBandLoader
                                ) extends ConfigLoader[CustomerDemographicsConfig] {

  override def loadConfiguration(config: Config): CustomerDemographicsConfig = {

    val genderConfig = genderLoader.loadConfiguration(config.getConfig(DataGenerationConstants.GENDER))

    val ageBandsConfig = ageBandLoader.loadConfiguration(config)

    CustomerDemographicsConfig(
      ageBands = ageBandsConfig,
      gender = genderConfig
    )
  }
}