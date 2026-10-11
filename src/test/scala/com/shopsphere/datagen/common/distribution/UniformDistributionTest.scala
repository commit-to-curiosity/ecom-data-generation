package com.shopsphere.datagen.common.distribution

import org.scalatest.funsuite.AnyFunSuite

class UniformDistributionTest extends AnyFunSuite {

  test("should reject empty values") {
    assertThrows[IllegalArgumentException] {
      new UniformDistribution[String](
        Seq.empty,
        new RandomGenerator(42L)
      )
    }
  }
}
