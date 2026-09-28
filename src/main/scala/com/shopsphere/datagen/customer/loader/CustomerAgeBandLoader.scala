package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.customer.config.AgeBandConfig
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

class CustomerAgeBandLoader extends ConfigLoader[Seq[AgeBandConfig]] {

  override def load(config: Config): Seq[AgeBandConfig] = {
    val ageBands = config.
      getConfigList("age_bands")
      .asScala
      .map { ageBand =>
        AgeBandConfig(
          minAge = ageBand.getInt("min_age"),
          maxAge = ageBand.getInt("max_age"),
          weight = ageBand.getInt("wight")
        )
      }.toSeq
    ageBands
  }
}
