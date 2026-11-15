
package com.shopsphere.datagen.customer.config

import org.scalatest.funsuite.AnyFunSuite

class CustomerDemographicsConfigValidatorTest extends AnyFunSuite {

  private val validAgeBands = Seq(
    CustomerAgeBandConfig(18, 24, 0.10),
    CustomerAgeBandConfig(25, 34, 0.25),
    CustomerAgeBandConfig(35, 44, 0.30),
    CustomerAgeBandConfig(45, 54, 0.20),
    CustomerAgeBandConfig(55, 64, 0.10),
    CustomerAgeBandConfig(65, 80, 0.05)
  )

  test("should accept valid contiguous age bands") {
    CustomerDemographicsConfigValidator.validate(validAgeBands)
  }

  test("should reject empty age bands") {
    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(Seq.empty)
    }
  }

  test("should reject negative minimum age") {
    val ageBands = Seq(
      CustomerAgeBandConfig(-1, 24, 1.0)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }

  test("should reject minimum age greater than maximum age") {
    val ageBands = Seq(
      CustomerAgeBandConfig(25, 18, 1.0)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }

  test("should reject overlapping age bands") {
    val ageBands = Seq(
      CustomerAgeBandConfig(18, 25, 0.50),
      CustomerAgeBandConfig(25, 34, 0.50)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }

  test("should reject gaps between age bands") {
    val ageBands = Seq(
      CustomerAgeBandConfig(18, 24, 0.50),
      CustomerAgeBandConfig(26, 34, 0.50)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }

  test("should reject population weights that do not sum to one") {
    val ageBands = Seq(
      CustomerAgeBandConfig(18, 24, 0.40),
      CustomerAgeBandConfig(25, 34, 0.40)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }

  test("should reject negative population weights") {
    val ageBands = Seq(
      CustomerAgeBandConfig(18, 24, -0.10),
      CustomerAgeBandConfig(25, 34, 1.10)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }

  test("should reject NaN population weights") {
    val ageBands = Seq(
      CustomerAgeBandConfig(18, 24, Double.NaN)
    )

    intercept[IllegalArgumentException] {
      CustomerDemographicsConfigValidator.validate(ageBands)
    }
  }
}
