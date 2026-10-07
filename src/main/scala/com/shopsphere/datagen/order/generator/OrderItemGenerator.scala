package com.shopsphere.datagen.order.generator

import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.order.model.{Order, OrderItem}
import com.shopsphere.datagen.product.model.Product
import com.shopsphere.datagen.product.reference.ProductReferenceData

class OrderItemGenerator(
                          order: Order,
                          productReferenceData: ProductReferenceData,
                          randomGenerator: RandomGenerator
                        ) extends Generator[OrderItem] {

  private var nextOrderItemId = 1L

  private val products =
    productReferenceData.products

  override def generate(): OrderItem = {
    val product =
      new UniformDistribution[Product](
        products,
        randomGenerator
      ).sample()

    val quantity =
      new UniformDistribution[Int](
        1 to 5,
        randomGenerator
      ).sample()

    val unitPrice = product.price
    val totalPrice = unitPrice * quantity

    OrderItem(
      id = generateOrderItemId(),
      orderId = order.id,
      productId = product.id,
      quantity = quantity,
      unitPrice = unitPrice,
      totalPrice = totalPrice
    )
  }

  private def generateOrderItemId(): String = {
    val orderItemId = f"ITEM_$nextOrderItemId%06d"
    nextOrderItemId += 1
    orderItemId
  }
}