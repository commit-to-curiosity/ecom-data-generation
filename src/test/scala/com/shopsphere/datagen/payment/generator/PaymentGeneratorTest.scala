package com.shopsphere.datagen.payment.generator

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.PaymentStatus
import com.shopsphere.datagen.customer.model.Customer
import com.shopsphere.datagen.order.generator.OrderGenerator
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class PaymentGeneratorTest extends AnyFunSuite {

  test("PaymentGenerator should generate payment linked to order") {

    val customer = Customer(
      id = "CUST_000001",
      firstName = "John",
      lastName = "Doe",
      email = "john.doe@example.com",
      phone = "9999999999",
      gender = "male",
      dateOfBirth = LocalDate.of(1990, 1, 1),
      registrationDate = LocalDate.of(2025, 1, 1),
      customerStatus = "active",
      customerSegment = "standard",
      acquisitionChannel = "organic",
      acquisitionCampaign = "seo",
      preferredDevice = "mobile",
      preferredPaymentMethod = "upi"
    )

    val randomGenerator = new RandomGenerator(42)

    val order =
      new OrderGenerator(
        customer,
        randomGenerator
      ).generate()

    val payment =
      new PaymentGenerator(
        order,
        randomGenerator
      ).generate()

    assert(payment.id == "PAY_000001")
    assert(payment.orderId == order.id)
    assert(payment.paymentMethod.nonEmpty)
    assert(payment.paymentStatus.nonEmpty)
    assert(payment.amount == order.totalAmount)

    assert(!payment.paymentDate.isBefore(order.orderDate))
    assert(!payment.paymentDate.isAfter(LocalDate.of(2026, 1, 1)))
  }

  test("PaymentGenerator should generate requested payment status") {

    val customer = Customer(
      id = "CUST_000001",
      firstName = "John",
      lastName = "Doe",
      email = "john.doe@example.com",
      phone = "9999999999",
      gender = "male",
      dateOfBirth = LocalDate.of(1990, 1, 1),
      registrationDate = LocalDate.of(2025, 1, 1),
      customerStatus = "active",
      customerSegment = "standard",
      acquisitionChannel = "organic",
      acquisitionCampaign = "seo",
      preferredDevice = "mobile",
      preferredPaymentMethod = "upi"
    )

    val randomGenerator = new RandomGenerator(42)

    val order =
      new OrderGenerator(
        customer,
        randomGenerator
      ).generate()

    val payment =
      new PaymentGenerator(
        order,
        randomGenerator
      ).generate(PaymentStatus.FAILED)

    assert(payment.paymentStatus == PaymentStatus.FAILED.name)
    assert(payment.orderId == order.id)
    assert(payment.amount == order.totalAmount)
  }
}