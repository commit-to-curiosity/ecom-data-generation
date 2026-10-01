package com.shopsphere.datagen.customer.generation

import com.shopsphere.datagen.common.distribution.WeightedDistribution
import com.shopsphere.datagen.common.generation.Generator
import com.shopsphere.datagen.common.random.RandomGenerator
import com.shopsphere.datagen.customer.config.CustomerStatusConfig
import com.shopsphere.datagen.customer.model.CustomerStatus

class CustomerStatusGenerator(
                               statusConfig: CustomerStatusConfig,
                               random: RandomGenerator
                             ) extends Generator[CustomerStatus] {

  private val statusDistribution =
    new WeightedDistribution[CustomerStatus](
      Seq(
        CustomerStatus.Active -> statusConfig.active,
        CustomerStatus.Inactive -> statusConfig.inactive,
        CustomerStatus.Suspended -> statusConfig.suspended,
        CustomerStatus.Closed -> statusConfig.closed
      ),
      random
    )

  override def generate(): CustomerStatus = {
    statusDistribution.sample()
  }
}