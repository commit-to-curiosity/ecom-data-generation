package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.GenderDistributionConfig
import com.typesafe.config.Config

class GenderDistributionLoader extends ConfigLoader[GenderDistributionConfig] {

  override def load(config: Config): GenderDistributionConfig = {
    val genderConfig = config.getConfig("gender")
    GenderDistributionConfig(
      male = genderConfig.getDouble("male"),
      female = genderConfig.getDouble("female"),
      other = genderConfig.getDouble("other")
    )
  }

}
