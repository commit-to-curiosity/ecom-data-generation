package com.shopsphere.datagen.payment.generator

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class PaymentIdGenerator {

  private val nextIds = new ConcurrentHashMap[String, AtomicLong]()

  def generate(orderId: String): String = {
    require(orderId != null && orderId.nonEmpty, "Order ID must not be empty")

    val sequence =
      nextIds.computeIfAbsent(orderId, _ => new AtomicLong(1L)).getAndIncrement()

    f"PAY${sequence}%06d_${orderId}"
  }
}
