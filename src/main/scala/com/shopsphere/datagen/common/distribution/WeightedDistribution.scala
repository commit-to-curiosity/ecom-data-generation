package com.shopsphere.datagen.common.distribution

class WeightedDistribution[T](
                               values: Seq[(T, Double)],
                               randomGenerator: RandomGenerator
                             ) extends Distribution[T] {

  require(values.nonEmpty, "Weighted distribution values must not be empty")
  require(
    values.forall { case (_, weight) => weight >= 0.0 },
    "Weighted distribution weights must not be negative"
  )

  private val totalWeight = values.map(_._2).sum

  require(totalWeight > 0.0, "Weighted distribution total weight must be positive")

  override def sample(): T = {
    val randomValue = randomGenerator.nextDouble() * totalWeight

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
