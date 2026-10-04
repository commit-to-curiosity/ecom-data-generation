package com.shopsphere.datagen.common.distribution

class WeightedDistribution[T](
                               values: Seq[(T, Double)],
                               randomGenerator: RandomGenerator
                             ) extends Distribution[T] {

  override def sample(): T = {
    val randomValue = randomGenerator.nextDouble()

    var cumulativeWeight = 0.0

    values.find { case (_, weight) =>
      cumulativeWeight += weight
      randomValue < cumulativeWeight
    } match {
      case Some((value, _)) => value
      case None =>
        throw new IllegalStateException("Unable to sample from weighted distribution")
    }
  }
}