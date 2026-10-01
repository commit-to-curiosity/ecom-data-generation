package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition.ReferralCampaignConfig
import com.typesafe.config.Config

class ReferralCampaignLoader extends ConfigLoader[ReferralCampaignConfig] {

  override def load(config: Config): ReferralCampaignConfig = {
    ReferralCampaignConfig(
      customerReferral = config.getDouble("customer_referral")
    )
  }
}
