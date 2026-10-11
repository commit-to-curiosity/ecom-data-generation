package com.shopsphere.datagen.order.generator

class OrderItemIdGenerator {

  private var nextOrderItemId = 1L

  def generate(): String = {
    val orderItemId = f"ITEM_$nextOrderItemId%06d"
    nextOrderItemId += 1
    orderItemId
  }
}