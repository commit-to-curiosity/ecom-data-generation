package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.{CustomerConfig, CustomerDemographicsConfig}
import com.shopsphere.datagen.customer.loader.acquisition.CustomerAcquisitionLoader
import com.shopsphere.datagen.customer.loader.behavior.CustomerBehaviorLoader
import com.typesafe.config.Config

class CustomerConfigLoader(
                            demographicsLoader: CustomerDemographicsLoader,
                            lifecycleLoader: CustomerLifecycleLoader,
                            segmentLoader: CustomerSegmentLoader,
                            statusLoader: CustomerStatusLoader,
                            acquisitionLoader: CustomerAcquisitionLoader,
                            behaviorLoader: CustomerBehaviorLoader
                          ) extends ConfigLoader[CustomerConfig] {

  override def loadConfiguration(config: Config): CustomerConfig = {

    CustomerConfig(
      demographics = demographicsLoader.loadConfiguration(config.getConfig(DataGenerationConstants.DEMOGRAPHICS)),
      lifecycle = lifecycleLoader.loadConfiguration(config.getConfig(DataGenerationConstants.LIFECYCLE)),
      segment = segmentLoader.loadConfiguration(config.getConfig(DataGenerationConstants.SEGMENT)),
      status = statusLoader.loadConfiguration(config.getConfig(DataGenerationConstants.STATUS)),
      acquisition = acquisitionLoader.loadConfiguration(config.getConfig(DataGenerationConstants.ACQUISITION)),
      behavior = behaviorLoader.loadConfiguration(config.getConfig(DataGenerationConstants.BEHAVIOR))
    )
  }
}