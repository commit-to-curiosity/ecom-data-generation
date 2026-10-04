package com.shopsphere.datagen.common.distribution

class TriangularDistribution(
                              min: Double,
                              mode: Double,
                              max: Double,
                              randomGenerator: RandomGenerator
                            ) extends Distribution[Double] {

  override def sample(): Double = {
    val randomValue = randomGenerator.nextDouble()
    val range = max - min
    val modePosition = (mode - min) / range

    if (randomValue < modePosition) {
      min + math.sqrt(randomValue * range * (mode - min))
    } else {
      max - math.sqrt((1.0 - randomValue) * range * (max - mode))
    }
  }
}