package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, LoaderRegistry}
import com.shopsphere.datagen.customer.config.{AcquisitionConfig, AgeBandConfig, CustomerConfig, CustomerDemographicsConfig, CustomerLifecycleConfig, CustomerSegmentConfig, CustomerStatusConfig}
import com.typesafe.config.Config

class CustomerConfigLoader(
                            registry: LoaderRegistry
                          ) extends ConfigLoader[CustomerConfig] {

  override def load(config: Config): CustomerConfig = {
    val demographicsLoader = registry.get[CustomerDemographicsConfig]
    val ageBandLoader = registry.get[Seq[AgeBandConfig]]
    val lifecycleLoader = registry.get[CustomerLifecycleConfig]
    val statusLoader = registry.get[CustomerStatusConfig]
    val segmentLoader = registry.get[CustomerSegmentConfig]
    val acquisitionLoader = registry.get[AcquisitionConfig]

    val customerConfig = config.getConfig("customer")

    CustomerConfig(
      demographics =
        demographicsLoader.load(customerConfig.getConfig("demographics")),
      lifecycle =
        lifecycleLoader.load(customerConfig.getConfig("lifecycle")),
      status =
        statusLoader.load(customerConfig.getConfig("status")),
      segment =
        segmentLoader.load(customerConfig.getConfig("segment")),
      acquisition =
        acquisitionLoader.load(customerConfig.getConfig("acquisition"))
    )
  }
}