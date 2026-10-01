package com.shopsphere.datagen.customer.generation

import com.shopsphere.datagen.common.distribution.WeightedDistribution
import com.shopsphere.datagen.common.generation.Generator
import com.shopsphere.datagen.common.random.RandomGenerator
import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig

class CustomerAgeGenerator(
                            ageBands: Seq[CustomerAgeBandConfig],
                            random: RandomGenerator
                          ) extends Generator[Int] {

  private val ageBandDistribution =
    new WeightedDistribution(
      ageBands.map(ageBand => ageBand -> ageBand.weight),
      random
    )

  override def generate(): Int = {
    val ageBand = ageBandDistribution.sample()

    random.nextInt(
      ageBand.minAge,
      ageBand.maxAge
    )
  }
}