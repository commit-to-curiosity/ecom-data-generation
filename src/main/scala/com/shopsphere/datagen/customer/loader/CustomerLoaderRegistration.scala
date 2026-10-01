package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.LoaderRegistry
import com.shopsphere.datagen.customer.config.{CustomerAgeBandConfig, CustomerGenderConfig}
import com.shopsphere.datagen.customer.config.acquisition._
import com.shopsphere.datagen.customer.loader.acquisition._

object CustomerLoaderRegistration {

  def register(registry: LoaderRegistry): Unit = {
    registry.register(new CustomerAgeBandLoader)
    registry.register(new CustomerGenderLoader)

    registry.register(
      new CustomerDemographicsLoader(
        registry.get[Seq[CustomerAgeBandConfig]],
        registry.get[CustomerGenderConfig]
      )
    )
    registry.register(new CustomerLifecycleLoader)
    registry.register(new CustomerStatusLoader)
    registry.register(new CustomerSegmentLoader)

    registry.register(new AcquisitionChannelLoader)
    registry.register(new OrganicCampaignLoader)
    registry.register(new PaidSearchCampaignLoader)
    registry.register(new SocialCampaignLoader)
    registry.register(new EmailCampaignLoader)
    registry.register(new DirectCampaignLoader)
    registry.register(new ReferralCampaignLoader)

    registry.register(
      new AcquisitionCampaignLoader(
        registry.get[OrganicCampaignConfig],
        registry.get[PaidSearchCampaignConfig],
        registry.get[SocialCampaignConfig],
        registry.get[EmailCampaignConfig],
        registry.get[DirectCampaignConfig],
        registry.get[ReferralCampaignConfig]
      )
    )
    registry.register(
      new CustomerAcquisitionLoader(
        registry.get[AcquisitionChannelConfig],
        registry.get[AcquisitionCampaignConfig]
      )
    )
  }
}