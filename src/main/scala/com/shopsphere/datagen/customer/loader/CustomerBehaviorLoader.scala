package com.shopsphere.datagen.customer.loader.behavior

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.CustomerBehaviorConfig
import com.shopsphere.datagen.customer.loader.{CustomerPreferredDeviceLoader, CustomerPreferredPaymentMethodLoader}
import com.typesafe.config.Config

class CustomerBehaviorLoader(
                              preferredDeviceLoader: CustomerPreferredDeviceLoader,
                              preferredPaymentMethodLoader: CustomerPreferredPaymentMethodLoader
                            ) extends ConfigLoader[CustomerBehaviorConfig] {

  override def loadConfiguration(config: Config): CustomerBehaviorConfig = {
    CustomerBehaviorConfig(
      preferredDevice = preferredDeviceLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.PREFERRED_DEVICE)
      ),
      preferredPaymentMethod = preferredPaymentMethodLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.PREFERRED_PAYMENT_METHOD)
      )
    )
  }
}