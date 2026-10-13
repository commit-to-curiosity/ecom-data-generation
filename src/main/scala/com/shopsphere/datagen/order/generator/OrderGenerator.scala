package com.shopsphere.datagen.order.generator

import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.customer.model.Customer
import com.shopsphere.datagen.order.model.Order

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

class OrderGenerator(
                      customer: Customer,
                      randomGenerator: RandomGenerator,
                      asOfDate: LocalDate
                    ) extends Generator[Order] {

  private val nextOrderId = new AtomicLong(1L)

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

  def this(
            customer: Customer,
            randomGenerator: RandomGenerator
          ) = this(
    customer,
    randomGenerator,
    LocalDate.of(2026, 1, 1)
  )

  private def generateOrderId(): String = {
    f"ORD${nextOrderId.getAndIncrement()}%06d_${customer.id}"
  }

  private def generateOrderDate(): LocalDate = {
    val daysSinceRegistration =
      ChronoUnit.DAYS.between(customer.registrationDate, asOfDate).toInt

    val orderDay =
      new UniformDistribution(
        0 to daysSinceRegistration,
        randomGenerator
      ).sample()

    customer.registrationDate.plusDays(orderDay.toLong)
  }
}
