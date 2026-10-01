package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.EmailCampaignConfig
import com.typesafe.config.Config

class EmailCampaignLoader extends ConfigLoader[EmailCampaignConfig] {

  override def load(config: Config): EmailCampaignConfig = {
    EmailCampaignConfig(
      newsletter = config.getDouble("newsletter"),
      promotion = config.getDouble("promotion")
    )
  }
}
