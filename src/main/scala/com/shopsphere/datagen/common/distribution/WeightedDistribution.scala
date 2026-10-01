package com.shopsphere.datagen.common.distribution

import com.shopsphere.datagen.common.random.RandomGenerator

class WeightedDistribution[T](
                               values: Seq[(T, Double)],
                               random: RandomGenerator
                             ) extends Distribution[T] {

  require(values.nonEmpty, "Weighted distribution must contain at least one value")
  require(
    values.forall(_._2 >= 0.0),
    "Weighted distribution cannot contain negative weights"
  )

  private val totalWeight: Double =
    values.map(_._2).sum

  require(
    totalWeight > 0.0,
    "Weighted distribution must have a total weight greater than zero"
  )

  override def sample(): T = {
    val target = random.nextDouble() * totalWeight

    var cumulativeWeight = 0.0

    values.foreach { case (value, weight) =>
      cumulativeWeight += weight

      if (target < cumulativeWeight) {
        return value
      }
    }

    values.last._1
  }
}