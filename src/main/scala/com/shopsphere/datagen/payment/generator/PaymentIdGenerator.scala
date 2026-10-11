package com.shopsphere.datagen.payment.generator

import java.util.concurrent.atomic.AtomicLong

class PaymentIdGenerator {

  private val nextPaymentId = new AtomicLong(1L)

  def generate(): String = {
    f"PAY_${nextPaymentId.getAndIncrement()}%06d"
  }
}
