package com.shopsphere.datagen.customer.generation

import com.shopsphere.datagen.common.distribution.WeightedDistribution
import com.shopsphere.datagen.common.generation.Generator
import com.shopsphere.datagen.common.random.RandomGenerator
import com.shopsphere.datagen.customer.config.CustomerGenderConfig
import com.shopsphere.datagen.customer.model.Gender

class CustomerGenderGenerator(
                               genderConfig: CustomerGenderConfig,
                               random: RandomGenerator
                             ) extends Generator[Gender] {

  private val genderDistribution =
    new WeightedDistribution[Gender](
      Seq(
        Gender.Male -> genderConfig.male,
        Gender.Female -> genderConfig.female,
        Gender.Other -> genderConfig.other
      ),
      random
    )

  override def generate(): Gender = {
    genderDistribution.sample()
  }
}