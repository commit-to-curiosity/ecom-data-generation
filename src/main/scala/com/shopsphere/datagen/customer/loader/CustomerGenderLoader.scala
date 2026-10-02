package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.Gender
import com.shopsphere.datagen.customer.config.CustomerGenderConfig
import com.typesafe.config.Config

class CustomerGenderLoader extends ConfigLoader[CustomerGenderConfig] {

  override def loadConfiguration(config: Config): CustomerGenderConfig = {

    CustomerGenderConfig(
      male = config.getDouble(Gender.MALE.name),
      female = config.getDouble(Gender.FEMALE.name),
      other = config.getDouble(Gender.OTHER.name)
    )
  }
}