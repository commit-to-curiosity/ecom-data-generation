package com.shopsphere.datagen.payment.generator

import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution}
import com.shopsphere.datagen.common.enums.{PaymentMethod, PaymentStatus}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.order.model.Order
import com.shopsphere.datagen.payment.model.Payment

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PaymentGenerator(
                        order: Order,
                        randomGenerator: RandomGenerator
                      ) extends Generator[Payment] {

  private var nextPaymentId = 1L

  private val paymentMethodDistribution =
    new UniformDistribution[PaymentMethod](
      PaymentMethod.all,
      randomGenerator
    )

  private val paymentStatusDistribution =
    new UniformDistribution[PaymentStatus](
      PaymentStatus.all,
      randomGenerator
    )

  override def generate(): Payment = {
    val paymentDate = generatePaymentDate()
    val paymentMethod = paymentMethodDistribution.sample()
    val paymentStatus = paymentStatusDistribution.sample()

    Payment(
      id = generatePaymentId(),
      orderId = order.id,
      paymentDate = paymentDate,
      paymentMethod = paymentMethod.name,
      paymentStatus = paymentStatus.name,
      amount = order.totalAmount
    )
  }

  private def generatePaymentId(): String = {
    val paymentId = f"PAY_$nextPaymentId%06d"
    nextPaymentId += 1
    paymentId
  }

  private def generatePaymentDate(): LocalDate = {
    val asOfDate = LocalDate.of(2026, 1, 1)

    val daysSinceOrder =
      ChronoUnit.DAYS.between(order.orderDate, asOfDate).toInt

    val paymentDay =
      new UniformDistribution(
        0 to daysSinceOrder,
        randomGenerator
      ).sample()

    order.orderDate.plusDays(paymentDay.toLong)
  }
}