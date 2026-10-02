package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.CustomerLifecycleConfig
import com.typesafe.config.Config

import java.time.LocalDate

class CustomerLifecycleLoader
  extends ConfigLoader[CustomerLifecycleConfig] {

  override def loadConfiguration(config: Config): CustomerLifecycleConfig = {
    CustomerLifecycleConfig(
      asOfDate = LocalDate.parse(config.getString(DataGenerationConstants.AS_OF_DATE)),
      registrationHistoryDays = config.getInt(DataGenerationConstants.REGISTRATION_HISTORY_DAYS)
    )
  }
}