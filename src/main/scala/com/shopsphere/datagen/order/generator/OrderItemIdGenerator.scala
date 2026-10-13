package com.shopsphere.datagen.order.generator

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class OrderItemIdGenerator {

  private val nextIds = new ConcurrentHashMap[String, AtomicLong]()

  def generate(orderId: String): String = {
    require(orderId != null && orderId.nonEmpty, "Order ID must not be empty")

    val sequence =
      nextIds.computeIfAbsent(orderId, _ => new AtomicLong(1L)).getAndIncrement()

    f"ITEM${sequence}%06d_${orderId}"
  }
}
