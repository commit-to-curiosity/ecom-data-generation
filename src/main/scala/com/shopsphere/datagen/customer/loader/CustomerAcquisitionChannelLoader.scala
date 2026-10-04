package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.common.enums.CustomerAcquisitionChannel
import com.shopsphere.datagen.customer.config.acquisition.AcquisitionChannelConfig
import com.typesafe.config.Config

class CustomerAcquisitionChannelLoader
  extends ConfigLoader[AcquisitionChannelConfig] {

  override def loadConfiguration(config: Config): AcquisitionChannelConfig = {
    AcquisitionChannelConfig(
      organic = config.getDouble(CustomerAcquisitionChannel.ORGANIC.name),
      paidSearch = config.getDouble(CustomerAcquisitionChannel.PAID_SEARCH.name),
      social = config.getDouble(CustomerAcquisitionChannel.SOCIAL.name),
      email = config.getDouble(CustomerAcquisitionChannel.EMAIL.name),
      direct = config.getDouble(CustomerAcquisitionChannel.DIRECT.name),
      referral = config.getDouble(CustomerAcquisitionChannel.REFERRAL.name)
    )
  }
}