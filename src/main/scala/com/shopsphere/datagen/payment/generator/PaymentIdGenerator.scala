package com.shopsphere.datagen.payment.generator

class PaymentIdGenerator {

  private var nextPaymentId = 1L

  def generate(): String = {
    val paymentId = f"PAY_$nextPaymentId%06d"
    nextPaymentId += 1
    paymentId
  }
}