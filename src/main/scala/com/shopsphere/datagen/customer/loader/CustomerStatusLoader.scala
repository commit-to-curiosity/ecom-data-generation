package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.common.enums.CustomerStatus
import com.shopsphere.datagen.customer.config.CustomerStatusConfig
import com.typesafe.config.Config

class CustomerStatusLoader extends ConfigLoader[CustomerStatusConfig] {

  override def loadConfiguration(config: Config): CustomerStatusConfig = {

    CustomerStatusConfig(
      active = config.getDouble(CustomerStatus.ACTIVE.name),
      inactive = config.getDouble(CustomerStatus.INACTIVE.name),
      suspended = config.getDouble(CustomerStatus.SUSPENDED.name),
      closed = config.getDouble(CustomerStatus.CLOSED.name)
    )
  }
}