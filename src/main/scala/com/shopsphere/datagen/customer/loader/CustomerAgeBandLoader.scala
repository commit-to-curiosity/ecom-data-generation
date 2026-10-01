package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

class CustomerAgeBandLoader extends ConfigLoader[Seq[CustomerAgeBandConfig]] {

  override def load(config: Config): Seq[CustomerAgeBandConfig] = {
    val ageBands = config.
      getConfigList("age_bands")
      .asScala
      .map { ageBand =>
        CustomerAgeBandConfig(
          minAge = ageBand.getInt("min_age"),
          maxAge = ageBand.getInt("max_age"),
          weight = ageBand.getInt("weight")
        )
      }.toSeq
    ageBands
  }
}
