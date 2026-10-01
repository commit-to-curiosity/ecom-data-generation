package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.DirectCampaignConfig
import com.typesafe.config.Config

class DirectCampaignLoader extends ConfigLoader[DirectCampaignConfig] {

  override def load(config: Config): DirectCampaignConfig = {
    DirectCampaignConfig(
      none = config.getDouble("none")
    )
  }
}
