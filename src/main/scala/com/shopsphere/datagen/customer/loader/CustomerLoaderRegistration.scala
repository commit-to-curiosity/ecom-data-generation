package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.LoaderRegistry
import com.shopsphere.datagen.customer.config.AgeBandConfig

object CustomerLoaderRegistration {

  def register(registry: LoaderRegistry): Unit = {
    registry.register(new CustomerAgeBandLoader)
    val ageBandLoader =
      registry.get[Seq[AgeBandConfig]]
    registry.register(new CustomerDemographicsLoader(ageBandLoader))
    registry.register(new CustomerLifecycleLoader)
    registry.register(new CustomerStatusLoader)
    registry.register(new CustomerSegmentLoader)
    registry.register(new CustomerAcquisitionLoader)
  }
}