package com.shopsphere.datagen.common.distribution

import java.util.Random

class RandomGenerator(seed: Long) {
  private val random = new java.util.Random(seed)

  def nextDouble(): Double = {
    random.nextDouble()
  }

  def nextInt(bound: Int): Int = {
    random.nextInt(bound)
  }

  def randomInstance: Random = {
    random
  }
}