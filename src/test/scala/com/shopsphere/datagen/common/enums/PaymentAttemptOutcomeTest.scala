package com.shopsphere.datagen.common.enums

import com.shopsphere.datagen.common.enums.{PaymentAttemptOutcome, PaymentStatus}

import org.scalatest.funsuite.AnyFunSuite

class PaymentAttemptOutcomeTest extends AnyFunSuite {

  test("PaymentAttemptOutcome should define correct payment status sequences") {

    assert(
      PaymentAttemptOutcome.SUCCESS_ON_FIRST_ATTEMPT.paymentStatuses == Seq(PaymentStatus.SUCCESSFUL)
    )

    assert(
      PaymentAttemptOutcome.SUCCESS_AFTER_ONE_RETRY.paymentStatuses ==
        Seq(
          PaymentStatus.FAILED,
          PaymentStatus.SUCCESSFUL
        )
    )

    assert(
      PaymentAttemptOutcome.SUCCESS_AFTER_TWO_RETRIES.paymentStatuses ==
        Seq(
          PaymentStatus.FAILED,
          PaymentStatus.FAILED,
          PaymentStatus.SUCCESSFUL
        )
    )

    assert(
      PaymentAttemptOutcome.PERMANENT_FAILURE.paymentStatuses ==
        Seq(
          PaymentStatus.FAILED,
          PaymentStatus.FAILED,
          PaymentStatus.FAILED
        )
    )
  }

  test("PaymentAttemptOutcome attempt count should match status sequence") {

    PaymentAttemptOutcome.all.foreach { outcome =>
      assert(
        outcome.attemptCount == outcome.paymentStatuses.size
      )
    }
  }
}