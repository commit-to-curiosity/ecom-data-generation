package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.common.enums.PreferredDevice
import com.shopsphere.datagen.customer.config.behavior.CustomerPreferredDeviceConfig
import com.typesafe.config.Config

class CustomerPreferredDeviceLoader
  extends ConfigLoader[CustomerPreferredDeviceConfig] {

  override def loadConfiguration(
                                  config: Config
                                ): CustomerPreferredDeviceConfig = {
    CustomerPreferredDeviceConfig(
      mobile = config.getDouble(PreferredDevice.MOBILE.name),
      desktop = config.getDouble(PreferredDevice.DESKTOP.name),
      tablet = config.getDouble(PreferredDevice.TABLET.name)
    )
  }
}