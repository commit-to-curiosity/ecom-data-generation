package com.shopsphere.datagen.customer.generator

import org.scalatest.funsuite.AnyFunSuite

class CustomerIdGeneratorTest extends AnyFunSuite {

  test("should generate canonical customer IDs") {
    assert(CustomerIdGenerator.generate(1L) == "CUST_000001")
    assert(CustomerIdGenerator.generate(42L) == "CUST_000042")
  }

  test("should reject non-positive ordinals") {
    assertThrows[IllegalArgumentException] {
      CustomerIdGenerator.generate(0L)
    }
  }
}
