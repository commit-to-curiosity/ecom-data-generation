package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.common.enums.CustomerSegment
import com.shopsphere.datagen.customer.config.CustomerSegmentConfig
import com.typesafe.config.Config

class CustomerSegmentLoader
  extends ConfigLoader[CustomerSegmentConfig] {

  override def loadConfiguration(
                                  config: Config
                                ): CustomerSegmentConfig = {

    CustomerSegmentConfig(
      standard = config.getDouble(CustomerSegment.STANDARD.name),
      premium = config.getDouble(CustomerSegment.PREMIUM.name),
      vip = config.getDouble(CustomerSegment.VIP.name),
      business = config.getDouble(CustomerSegment.BUSINESS.name)
    )
  }
}