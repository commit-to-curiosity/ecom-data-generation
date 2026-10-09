package com.shopsphere.datagen.payment.generator

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.PaymentAttemptOutcome
import org.scalatest.funsuite.AnyFunSuite

class PaymentAttemptOutcomeGeneratorTest extends AnyFunSuite {

  test("PaymentAttemptOutcomeGenerator should generate a valid payment outcome") {

    val randomGenerator =
      new RandomGenerator(42)

    val generator =
      new PaymentAttemptOutcomeGenerator(
        randomGenerator
      )

    val outcome =
      generator.generate()

    assert(
      PaymentAttemptOutcome.all.contains(outcome)
    )
  }
}