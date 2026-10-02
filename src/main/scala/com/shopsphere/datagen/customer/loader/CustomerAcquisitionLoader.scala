package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.CustomerAcquisitionConfig
import com.typesafe.config.Config

class CustomerAcquisitionLoader(
                                 channelLoader: CustomerAcquisitionChannelLoader,
                                 campaignLoader: CustomerAcquisitionCampaignLoader
                               ) extends ConfigLoader[CustomerAcquisitionConfig] {

  override def loadConfiguration(config: Config): CustomerAcquisitionConfig = {
    CustomerAcquisitionConfig(
      channel = channelLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.CHANNEL)
      ),
      campaign = campaignLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.CAMPAIGN)
      )
    )
  }
}