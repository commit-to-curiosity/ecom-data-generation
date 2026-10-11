package com.shopsphere.datagen.order.generator

import java.util.concurrent.atomic.AtomicLong

class OrderItemIdGenerator {

  private val nextOrderItemId = new AtomicLong(1L)

  def generate(): String = {
    f"ITEM_${nextOrderItemId.getAndIncrement()}%06d"
  }
}
