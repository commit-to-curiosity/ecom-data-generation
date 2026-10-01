package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.CustomerGenderConfig
import com.typesafe.config.Config

class CustomerGenderLoader extends ConfigLoader[CustomerGenderConfig] {

  override def load(config: Config): CustomerGenderConfig = {
    val genderConfig = config.getConfig("gender")
    CustomerGenderConfig(
      male = genderConfig.getDouble("male"),
      female = genderConfig.getDouble("female"),
      other = genderConfig.getDouble("other")
    )
  }

}
