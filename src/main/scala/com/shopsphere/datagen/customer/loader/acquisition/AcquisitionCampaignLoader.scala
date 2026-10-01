package com.shopsphere.datagen.customer.loader.acquisition

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.acquisition._
import com.typesafe.config.Config

class AcquisitionCampaignLoader(
                                 organicLoader: ConfigLoader[OrganicCampaignConfig],
                                 paidSearchLoader: ConfigLoader[PaidSearchCampaignConfig],
                                 socialLoader: ConfigLoader[SocialCampaignConfig],
                                 emailLoader: ConfigLoader[EmailCampaignConfig],
                                 directLoader: ConfigLoader[DirectCampaignConfig],
                                 referralLoader: ConfigLoader[ReferralCampaignConfig]
                               ) extends ConfigLoader[AcquisitionCampaignConfig] {

  override def load(config: Config): AcquisitionCampaignConfig = {
    AcquisitionCampaignConfig(
      organic = organicLoader.load(config.getConfig("organic")),
      paidSearch = paidSearchLoader.load(config.getConfig("paid_search")),
      social = socialLoader.load(config.getConfig("social")),
      email = emailLoader.load(config.getConfig("email")),
      direct = directLoader.load(config.getConfig("direct")),
      referral = referralLoader.load(config.getConfig("referral"))
    )
  }
}
