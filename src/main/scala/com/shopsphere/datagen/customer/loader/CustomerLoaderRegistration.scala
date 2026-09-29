package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.LoaderRegistry
import com.shopsphere.datagen.customer.config.{AgeBandConfig, GenderDistributionConfig}

object CustomerLoaderRegistration {

  def register(registry: LoaderRegistry): Unit = {
    registry.register(new CustomerAgeBandLoader)
    registry.register(new GenderDistributionLoader)

    val ageBandLoader = registry.get[Seq[AgeBandConfig]]
    val genderDistributionLoader = registry.get[GenderDistributionConfig]

    registry.register(
      new CustomerDemographicsLoader(
        ageBandLoader,
        genderDistributionLoader
      )
    )

    registry.register(new CustomerLifecycleLoader)
    registry.register(new CustomerStatusLoader)
    registry.register(new CustomerSegmentLoader)
    registry.register(new CustomerAcquisitionLoader)
  }
}