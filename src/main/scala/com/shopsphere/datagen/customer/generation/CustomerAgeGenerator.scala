package com.shopsphere.datagen.customer.generation

import com.shopsphere.datagen.common.distribution.WeightedDistribution
import com.shopsphere.datagen.common.generation.Generator
import com.shopsphere.datagen.customer.config.AgeBandConfig

import scala.util.Random

class CustomerAgeGenerator(
                            ageBands: Seq[AgeBandConfig],
                            random: Random
                          ) extends Generator[Int] {

  private val distribution =
    new WeightedDistribution[AgeBandConfig](
      ageBands.map(ageBand => ageBand -> ageBand.weight),
      random
    )

  override def generate(): Int = {
    val ageBand = distribution.sample()

    ageBand.minAge +
      random.nextInt(ageBand.maxAge - ageBand.minAge + 1)
  }
}