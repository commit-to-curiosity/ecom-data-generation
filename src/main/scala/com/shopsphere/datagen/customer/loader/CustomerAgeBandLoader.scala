package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig
import com.typesafe.config.Config

class CustomerAgeBandLoader
  extends ConfigLoader[Seq[CustomerAgeBandConfig]] {

  override def loadConfiguration(config: Config): Seq[CustomerAgeBandConfig] = {

    config.getConfigList(DataGenerationConstants.AGE_BANDS)
      .toArray
      .toSeq
      .map { ageBandConfig =>
        val ageBand = ageBandConfig.asInstanceOf[Config]

        CustomerAgeBandConfig(
          minAge = ageBand.getInt(DataGenerationConstants.MIN_AGE),
          maxAge = ageBand.getInt(DataGenerationConstants.MAX_AGE),
          weight = ageBand.getDouble(DataGenerationConstants.WEIGHT)
        )
      }
  }
}