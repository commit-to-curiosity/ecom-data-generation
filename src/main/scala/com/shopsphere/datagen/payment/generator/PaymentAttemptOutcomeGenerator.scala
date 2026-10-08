package com.shopsphere.datagen.payment.generator

import com.shopsphere.datagen.common.distribution.{RandomGenerator, WeightedDistribution}
import com.shopsphere.datagen.common.enums.PaymentAttemptOutcome

class PaymentAttemptOutcomeGenerator(
                                      randomGenerator: RandomGenerator
                                    ) {

  private val distribution =
    new WeightedDistribution[PaymentAttemptOutcome](
      PaymentAttemptOutcome.all.map(
        outcome => outcome -> outcome.weight
      ),
      randomGenerator
    )

  def generate(): PaymentAttemptOutcome =
    distribution.sample()
}