package com.shopsphere.datagen.order.model

case class OrderItem(
                      id: String,
                      orderId: String,
                      productId: String,
                      quantity: Int,
                      unitPrice: BigDecimal,
                      totalPrice: BigDecimal
                    )