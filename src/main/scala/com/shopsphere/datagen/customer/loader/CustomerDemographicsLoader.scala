package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.{AgeBandConfig, CustomerDemographicsConfig, GenderDistributionConfig}
import com.typesafe.config.Config

class CustomerDemographicsLoader(ageBandLoader: ConfigLoader[Seq[AgeBandConfig]])
  extends ConfigLoader[CustomerDemographicsConfig] {

  override def load(config: Config): CustomerDemographicsConfig = {
    val gender = config.getConfig("gender")

    CustomerDemographicsConfig(
      ageBands = ageBandLoader.load(config),
      gender = GenderDistributionConfig(
        male = gender.getDouble("male"),
        female = gender.getDouble("female"),
        other = gender.getDouble("other")
      )
    )
  }
}