package com.shopsphere.datagen.customer.generator

object CustomerIdGenerator {

  def generate(ordinal: Long): String = {
    require(ordinal > 0, "Customer ordinal must be positive")
    f"CUST$ordinal%06d"
  }
}
