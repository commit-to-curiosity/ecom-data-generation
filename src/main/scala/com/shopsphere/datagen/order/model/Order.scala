package com.shopsphere.datagen.order.model

import java.time.LocalDate

case class Order(
                  id: String,
                  customerId: String,
                  orderDate: LocalDate,
                  orderStatus: String,
                  totalAmount: BigDecimal
                )