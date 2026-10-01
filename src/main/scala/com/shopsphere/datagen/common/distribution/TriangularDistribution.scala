package com.shopsphere.datagenerator.common.distribution

import com.shopsphere.datagen.common.distribution.Distribution
import com.shopsphere.datagen.common.random.RandomGenerator

class TriangularDistribution(
                              min: Double,
                              mode: Double,
                              max: Double,
                              random: RandomGenerator
                            ) extends Distribution[Double] {

  require(
    min <= mode,
    "Triangular distribution requires min <= mode"
  )

  require(
    mode <= max,
    "Triangular distribution requires mode <= max"
  )

  require(
    min < max,
    "Triangular distribution requires min < max"
  )

  override def sample(): Double = {
    val u = random.nextDouble()
    val range = max - min
    val modePosition = (mode - min) / range

    if (u < modePosition) {
      min + math.sqrt(u * range * (mode - min))
    } else {
      max - math.sqrt((1.0 - u) * range * (max - mode))
    }
  }
}