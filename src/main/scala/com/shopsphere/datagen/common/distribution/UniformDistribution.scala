package com.shopsphere.datagen.common.distribution

class UniformDistribution[T](
                              values: Seq[T],
                              randomGenerator: RandomGenerator
                            ) extends Distribution[T] {

  override def sample(): T = {
    val index = (randomGenerator.nextDouble() * values.size).toInt
    values(index)
  }
}