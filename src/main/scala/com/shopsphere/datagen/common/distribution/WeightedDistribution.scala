package com.shopsphere.datagen.common.distribution

class WeightedDistribution[T](
                               values: Seq[(T, Double)],
                               randomGenerator: RandomGenerator
                             ) extends Distribution[T] {

  require(
    values.forall { case (_, weight) =>
      !weight.isNaN && !weight.isInfinity && weight >= 0.0
    },
    "Weighted distribution weights must be finite and non-negative"
  )

  private val totalWeight = values.map(_._2).sum

  require(
    !totalWeight.isNaN && !totalWeight.isInfinity && totalWeight > 0.0,
    "Weighted distribution total weight must be finite and positive"
  )

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
