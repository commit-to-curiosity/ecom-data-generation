package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.CustomerAcquisitionConfig
import com.shopsphere.datagen.customer.config.acquisition.{AcquisitionCampaignConfig, AcquisitionChannelConfig}
import com.typesafe.config.Config

class CustomerAcquisitionLoader(
                                 channelLoader: ConfigLoader[AcquisitionChannelConfig],
                                 campaignLoader: ConfigLoader[AcquisitionCampaignConfig]
                               )
  extends ConfigLoader[CustomerAcquisitionConfig] {

  override def load(config: Config): CustomerAcquisitionConfig = {
    CustomerAcquisitionConfig(
      acquisitionChannel = channelLoader.load(config.getConfig("channel")),
      acquisitionCampaign = campaignLoader.load(config.getConfig("campaign"))
    )
  }
}