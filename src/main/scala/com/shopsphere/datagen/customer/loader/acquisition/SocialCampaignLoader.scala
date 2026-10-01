package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.SocialCampaignConfig
import com.typesafe.config.Config

class SocialCampaignLoader extends ConfigLoader[SocialCampaignConfig] {

  override def load(config: Config): SocialCampaignConfig = {
    SocialCampaignConfig(
      instagram = config.getDouble("instagram"),
      facebook = config.getDouble("facebook"),
      youtube = config.getDouble("youtube")
    )
  }
}
