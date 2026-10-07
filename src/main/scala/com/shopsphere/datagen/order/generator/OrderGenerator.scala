package com.shopsphere.datagen.order.generator

import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.customer.model.Customer
import com.shopsphere.datagen.order.model.Order

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class OrderGenerator(
                      customer: Customer,
                      randomGenerator: RandomGenerator
                    ) extends Generator[Order] {

  private var nextOrderId = 1L

  override def generate(): Order = {
    val orderDate = generateOrderDate()

    Order(
      id = generateOrderId(),
      customerId = customer.id,
      orderDate = orderDate,
      orderStatus = "completed",
      totalAmount = BigDecimal("0.00")
    )
  }

  private def generateOrderId(): String = {
    val orderId = f"ORD_$nextOrderId%06d"
    nextOrderId += 1
    orderId
  }

  private def generateOrderDate(): LocalDate = {
    val asOfDate = LocalDate.of(2026, 1, 1)
    val daysSinceRegistration = ChronoUnit.DAYS.between(customer.registrationDate, asOfDate).toInt

    val orderDay =
      new UniformDistribution(
        0 to daysSinceRegistration,
        randomGenerator
      ).sample()

    customer.registrationDate.plusDays(orderDay.toLong)
  }
}