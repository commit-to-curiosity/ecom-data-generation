package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.PaidSearchCampaignConfig
import com.typesafe.config.Config

class PaidSearchCampaignLoader extends ConfigLoader[PaidSearchCampaignConfig] {

  override def load(config: Config): PaidSearchCampaignConfig = {
    PaidSearchCampaignConfig(
      brand = config.getDouble("brand"),
      generic = config.getDouble("generic")
    )
  }
}
