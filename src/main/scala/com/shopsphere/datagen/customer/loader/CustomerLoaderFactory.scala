package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.customer.loader.behavior.CustomerBehaviorLoader

object CustomerLoaderFactory {

  def createCustomerConfigLoader(): CustomerConfigLoader = {

    val genderLoader = new CustomerGenderLoader()
    val ageBandLoader = new CustomerAgeBandLoader()
    val lifecycleLoader = new CustomerLifecycleLoader()
    val segmentLoader = new CustomerSegmentLoader()
    val statusLoader = new CustomerStatusLoader()

    val demographicsLoader = new CustomerDemographicsLoader(
      genderLoader,
      ageBandLoader
    )

    val acquisitionChannelLoader = new CustomerAcquisitionChannelLoader()
    val acquisitionCampaignLoader = new CustomerAcquisitionCampaignLoader()

    val acquisitionLoader = new CustomerAcquisitionLoader(
      acquisitionChannelLoader,
      acquisitionCampaignLoader
    )

    val preferredDeviceLoader = new CustomerPreferredDeviceLoader()
    val preferredPaymentMethodLoader = new CustomerPreferredPaymentMethodLoader()

    val behaviorLoader = new CustomerBehaviorLoader(
      preferredDeviceLoader,
      preferredPaymentMethodLoader
    )

    new CustomerConfigLoader(
      demographicsLoader,
      lifecycleLoader,
      segmentLoader,
      statusLoader,
      acquisitionLoader,
      behaviorLoader
    )
  }
}