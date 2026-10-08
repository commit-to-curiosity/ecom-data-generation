package com.shopsphere.datagen.common.enums

sealed trait PaymentAttemptOutcome {
  def attemptCount: Int
  def paymentStatuses: Seq[PaymentStatus]
  def weight: Double
}

object PaymentAttemptOutcome {

  case object SUCCESS_ON_FIRST_ATTEMPT extends PaymentAttemptOutcome {
    override val attemptCount: Int = 1
    override val weight: Double = 0.70
    override val paymentStatuses: Seq[PaymentStatus] =
      Seq(PaymentStatus.SUCCESSFUL)
  }

  case object SUCCESS_AFTER_ONE_RETRY extends PaymentAttemptOutcome {
    override val attemptCount: Int = 2
    override val weight: Double = 0.20
    override val paymentStatuses: Seq[PaymentStatus] =
      Seq(
        PaymentStatus.FAILED,
        PaymentStatus.SUCCESSFUL
      )
  }

  case object SUCCESS_AFTER_TWO_RETRIES extends PaymentAttemptOutcome {
    override val attemptCount: Int = 3
    override val weight: Double = 0.07
    override val paymentStatuses: Seq[PaymentStatus] =
      Seq(
        PaymentStatus.FAILED,
        PaymentStatus.FAILED,
        PaymentStatus.SUCCESSFUL
      )
  }

  case object PERMANENT_FAILURE extends PaymentAttemptOutcome {
    override val attemptCount: Int = 3
    override val weight: Double = 0.03
    override val paymentStatuses: Seq[PaymentStatus] =
      Seq(
        PaymentStatus.FAILED,
        PaymentStatus.FAILED,
        PaymentStatus.FAILED
      )
  }

  val all: Seq[PaymentAttemptOutcome] =
    Seq(
      SUCCESS_ON_FIRST_ATTEMPT,
      SUCCESS_AFTER_ONE_RETRY,
      SUCCESS_AFTER_TWO_RETRIES,
      PERMANENT_FAILURE
    )
}