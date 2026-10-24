
package com.shopsphere.datagen.payment.generator

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.PaymentAttemptOutcome
import com.shopsphere.datagen.scenario.PaymentOutcomeWeights
import org.scalatest.funsuite.AnyFunSuite

class PaymentAttemptOutcomeGeneratorTest extends AnyFunSuite {

  test("PaymentAttemptOutcomeGenerator should generate a valid payment outcome") {
    val randomGenerator =
      new RandomGenerator(42L)

    val weights =
      PaymentOutcomeWeights(
        successOnFirstAttempt = 0.70,
        successAfterOneRetry = 0.20,
        successAfterTwoRetries = 0.07,
        permanentFailure = 0.03
      )

    val generator =
      new PaymentAttemptOutcomeGenerator(
        randomGenerator,
        weights
      )

    val outcome =
      generator.generate()

    assert(
      PaymentAttemptOutcome.all.contains(outcome)
    )
  }

  test("should use configured payment outcome weights") {
    val weights =
      PaymentOutcomeWeights(
        successOnFirstAttempt = 0.0,
        successAfterOneRetry = 1.0,
        successAfterTwoRetries = 0.0,
        permanentFailure = 0.0
      )

    val generator =
      new PaymentAttemptOutcomeGenerator(
        new RandomGenerator(42L),
        weights
      )

    assert(
      generator.generate() ==
        PaymentAttemptOutcome.SUCCESS_AFTER_ONE_RETRY
    )
  }
}
