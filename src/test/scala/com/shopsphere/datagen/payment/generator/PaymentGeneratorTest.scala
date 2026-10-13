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
      id = "CUST000001",
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
        randomGenerator,
        new PaymentIdGenerator()
      ).generate()

    assert(payment.id == s"PAY000001_${order.id}")
    assert(payment.orderId == order.id)
    assert(payment.paymentMethod.nonEmpty)
    assert(payment.paymentStatus.nonEmpty)
    assert(payment.amount == order.totalAmount)

    assert(!payment.paymentDate.isBefore(order.orderDate))
    assert(!payment.paymentDate.isAfter(LocalDate.of(2026, 1, 1)))
  }

  test("PaymentGenerator should generate requested payment status") {

    val customer = Customer(
      id = "CUST000001",
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
        randomGenerator,
        new PaymentIdGenerator()
      ).generate(PaymentStatus.FAILED)

    assert(payment.paymentStatus == PaymentStatus.FAILED.name)
    assert(payment.orderId == order.id)
    assert(payment.amount == order.totalAmount)
  }

  test("PaymentGenerator should generate payment dates using the provided as-of date") {

    val customer = Customer(
      id = "CUST000001",
      firstName = "John",
      lastName = "Doe",
      email = "john.doe@example.com",
      phone = "9999999999",
      gender = "male",
      dateOfBirth = LocalDate.of(1990, 1, 1),
      registrationDate = LocalDate.of(2024, 1, 1),
      customerStatus = "active",
      customerSegment = "standard",
      acquisitionChannel = "organic",
      acquisitionCampaign = "seo",
      preferredDevice = "mobile",
      preferredPaymentMethod = "upi"
    )

    val asOfDate = LocalDate.of(2025, 1, 1)
    val randomGenerator = new RandomGenerator(42)

    val order =
      new OrderGenerator(
        customer,
        randomGenerator,
        asOfDate
      ).generate()

    val payment =
      new PaymentGenerator(
        order,
        randomGenerator,
        new PaymentIdGenerator(),
        asOfDate
      ).generate()

    assert(!payment.paymentDate.isBefore(order.orderDate))
    assert(!payment.paymentDate.isAfter(asOfDate))
  }
}
