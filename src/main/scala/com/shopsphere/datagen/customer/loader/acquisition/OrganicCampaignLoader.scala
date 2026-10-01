package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.OrganicCampaignConfig
import com.typesafe.config.Config

class OrganicCampaignLoader extends ConfigLoader[OrganicCampaignConfig] {

  override def load(config: Config): OrganicCampaignConfig = {
    OrganicCampaignConfig(
      seo = config.getDouble("seo"),
      content = config.getDouble("content")
    )
  }
}
