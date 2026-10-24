
package com.shopsphere.datagen.payment.generator

import com.shopsphere.datagen.common.distribution.{
  RandomGenerator,
  WeightedDistribution
}
import com.shopsphere.datagen.common.enums.PaymentAttemptOutcome
import com.shopsphere.datagen.scenario.PaymentOutcomeWeights

class PaymentAttemptOutcomeGenerator(
                                      randomGenerator: RandomGenerator,
                                      paymentOutcomeWeights: PaymentOutcomeWeights
                                    ) {

  private val configuredWeights: Map[PaymentAttemptOutcome, Double] =
    Map(
      PaymentAttemptOutcome.SUCCESS_ON_FIRST_ATTEMPT ->
        paymentOutcomeWeights.successOnFirstAttempt,
      PaymentAttemptOutcome.SUCCESS_AFTER_ONE_RETRY ->
        paymentOutcomeWeights.successAfterOneRetry,
      PaymentAttemptOutcome.SUCCESS_AFTER_TWO_RETRIES ->
        paymentOutcomeWeights.successAfterTwoRetries,
      PaymentAttemptOutcome.PERMANENT_FAILURE ->
        paymentOutcomeWeights.permanentFailure
    )

  private val distribution =
    new WeightedDistribution[PaymentAttemptOutcome](
      PaymentAttemptOutcome.all.map { outcome =>
        outcome -> configuredWeights(outcome)
      },
      randomGenerator
    )

  def generate(): PaymentAttemptOutcome =
    distribution.sample()
}
