package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.correlation.CustomerCorrelationsConfigValidator
import com.shopsphere.datagen.customer.config.{CustomerConfig, CustomerDemographicsConfig, CustomerDemographicsConfigValidator, CustomerGenerationConfig, CustomerGenerationStrategy}
import com.shopsphere.datagen.customer.loader.CustomerAcquisitionLoader
import com.shopsphere.datagen.customer.loader.behavior.CustomerBehaviorLoader
import com.typesafe.config.Config

class CustomerConfigLoader(
                            demographicsLoader: CustomerDemographicsLoader,
                            lifecycleLoader: CustomerLifecycleLoader,
                            segmentLoader: CustomerSegmentLoader,
                            statusLoader: CustomerStatusLoader,
                            acquisitionLoader: CustomerAcquisitionLoader,
                            behaviorLoader: CustomerBehaviorLoader,
                            generationLoader: CustomerGenerationLoader
                          ) extends ConfigLoader[CustomerConfig] {

  override def loadConfiguration(config: Config): CustomerConfig = {

    val demographics =
      demographicsLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.DEMOGRAPHICS)
      )

    CustomerDemographicsConfigValidator.validate(demographics.ageBands)

    val lifecycle =
      lifecycleLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.LIFECYCLE)
      )

    val segment =
      segmentLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.SEGMENT)
      )

    val status =
      statusLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.STATUS)
      )

    val acquisition =
      acquisitionLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.ACQUISITION)
      )

    val behavior =
      behaviorLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.BEHAVIOR)
      )

    val generation =
      if (config.hasPath(DataGenerationConstants.GENERATION)) {
        generationLoader.loadConfiguration(
          config.getConfig(DataGenerationConstants.GENERATION)
        )
      } else {
        CustomerGenerationConfig()
      }

    if (generation.strategy == CustomerGenerationStrategy.CORRELATED) {
      CustomerCorrelationsConfigValidator.validate(
        generation.correlations.getOrElse(
          throw new IllegalArgumentException(
            "Correlations configuration is required for correlated generation"
          )
        ),
        demographics.ageBands,
        segment
      )
    }

    CustomerConfig(
      demographics = demographics,
      lifecycle = lifecycle,
      segment = segment,
      status = status,
      acquisition = acquisition,
      behavior = behavior,
      generation = generation
    )
  }
}