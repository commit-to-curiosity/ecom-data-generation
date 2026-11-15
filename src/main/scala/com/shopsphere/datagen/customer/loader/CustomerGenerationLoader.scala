package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.{
  CustomerGenerationConfig,
  CustomerGenerationStrategy
}
import com.typesafe.config.Config

class CustomerGenerationLoader(
                                correlationsLoader: CustomerCorrelationsLoader =
                                new CustomerCorrelationsLoader()
                              ) extends ConfigLoader[CustomerGenerationConfig] {

  override def loadConfiguration(
                                  config: Config
                                ): CustomerGenerationConfig = {

    val strategyName =
      if (config.hasPath(DataGenerationConstants.STRATEGY)) {
        config.getString(DataGenerationConstants.STRATEGY)
      } else {
        CustomerGenerationStrategy.INDEPENDENT.name
      }

    val strategy =
      CustomerGenerationStrategy.fromString(strategyName) match {
        case Right(value) => value
        case Left(error) => throw new IllegalArgumentException(error)
      }

    val correlations =
      if (config.hasPath(DataGenerationConstants.CORRELATIONS)) {
        Some(
          correlationsLoader.loadConfiguration(
            config.getConfig(DataGenerationConstants.CORRELATIONS)
          )
        )
      } else {
        None
      }

    require(
      strategy != CustomerGenerationStrategy.CORRELATED ||
        correlations.nonEmpty,
      "Correlations configuration is required for correlated generation"
    )

    CustomerGenerationConfig(
      strategy = strategy,
      correlations = correlations
    )
  }
}