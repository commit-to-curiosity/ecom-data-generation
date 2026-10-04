package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.common.enums.CustomerAcquisitionChannel
import com.shopsphere.datagen.customer.config.acquisition.{
  AcquisitionCampaignConfig,
  DirectCampaignConfig,
  EmailCampaignConfig,
  OrganicCampaignConfig,
  PaidSearchCampaignConfig,
  ReferralCampaignConfig,
  SocialCampaignConfig
}
import com.typesafe.config.Config

class CustomerAcquisitionCampaignLoader
  extends ConfigLoader[AcquisitionCampaignConfig] {

  override def loadConfiguration(
                                  config: Config
                                ): AcquisitionCampaignConfig = {

    AcquisitionCampaignConfig(
      organic = OrganicCampaignConfig(
        seo = config.getDouble(CustomerAcquisitionChannel.ORGANIC.SEO.path),
        content = config.getDouble(CustomerAcquisitionChannel.ORGANIC.CONTENT.path)
      ),
      paidSearch = PaidSearchCampaignConfig(
        brand = config.getDouble(CustomerAcquisitionChannel.PAID_SEARCH.BRAND.path),
        generic = config.getDouble(CustomerAcquisitionChannel.PAID_SEARCH.GENERIC.path)
      ),
      social = SocialCampaignConfig(
        instagram = config.getDouble(CustomerAcquisitionChannel.SOCIAL.INSTAGRAM.path),
        facebook = config.getDouble(CustomerAcquisitionChannel.SOCIAL.FACEBOOK.path),
        youtube = config.getDouble(CustomerAcquisitionChannel.SOCIAL.YOUTUBE.path)
      ),
      email = EmailCampaignConfig(
        newsletter = config.getDouble(CustomerAcquisitionChannel.EMAIL.NEWSLETTER.path),
        promotion = config.getDouble(CustomerAcquisitionChannel.EMAIL.PROMOTION.path)
      ),
      direct = DirectCampaignConfig(
        none = config.getDouble(CustomerAcquisitionChannel.DIRECT.NONE.path)
      ),
      referral = ReferralCampaignConfig(
        customerReferral = config.getDouble(CustomerAcquisitionChannel.REFERRAL.CUSTOMER_REFERRAL.path)
      )
    )
  }
}