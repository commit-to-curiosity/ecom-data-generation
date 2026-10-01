package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagen.common.distribution.Distribution
import com.shopsphere.datagen.common.random.RandomGenerator

class UniformDistribution[T](
                              values: Seq[T],
                              random: RandomGenerator
                            ) extends Distribution[T] {

  require(
    values.nonEmpty,
    "Uniform distribution must contain at least one value"
  )

  override def sample(): T = {
    values(random.nextInt(values.size))
  }
}