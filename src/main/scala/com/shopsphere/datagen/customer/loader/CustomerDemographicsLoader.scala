package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.{CustomerAgeBandConfig, CustomerDemographicsConfig, CustomerGenderConfig}
import com.typesafe.config.Config

class CustomerDemographicsLoader(
                                  ageBandLoader: ConfigLoader[Seq[CustomerAgeBandConfig]],
                                  genderDistributionLoader: ConfigLoader[CustomerGenderConfig])
  extends ConfigLoader[CustomerDemographicsConfig] {

  override def load(config: Config): CustomerDemographicsConfig = {

    CustomerDemographicsConfig(
      ageBands = ageBandLoader.load(config),
      gender = genderDistributionLoader.load(config)
    )
  }
}