package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.AcquisitionChannelConfig
import com.typesafe.config.Config

class AcquisitionChannelLoader extends ConfigLoader[AcquisitionChannelConfig] {

  override def load(config: Config): AcquisitionChannelConfig = {
    AcquisitionChannelConfig(
      organic = config.getDouble("organic"),
      paidSearch = config.getDouble("paid_search"),
      social = config.getDouble("social"),
      email = config.getDouble("email"),
      direct = config.getDouble("direct"),
      referral = config.getDouble("referral")
    )
  }
}
