package com.shopsphere.datagen.common.distribution

import org.scalatest.funsuite.AnyFunSuite

class WeightedDistributionTest extends AnyFunSuite {

  test("should sample when weights are not normalized") {
    val distribution =
      new WeightedDistribution[String](
        Seq(
          "a" -> 2.0,
          "b" -> 3.0
        ),
        new RandomGenerator(42L)
      )

    assert(Set("a", "b").contains(distribution.sample()))
  }

  test("should reject invalid weights") {
    assertThrows[IllegalArgumentException] {
      new WeightedDistribution[String](
        Seq(
          "a" -> 0.0,
          "b" -> 0.0
        ),
        new RandomGenerator(42L)
      )
    }

    assertThrows[IllegalArgumentException] {
      new WeightedDistribution[String](
        Seq(
          "a" -> 1.0,
          "b" -> -1.0
        ),
        new RandomGenerator(42L)
      )
    }
  }
}
