package com.shopsphere.datagen.order

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.order.generator.OrderGenerator
import com.shopsphere.datagen.customer.model.Customer
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class OrderGeneratorTest extends AnyFunSuite {

  test("should generate a valid order for a customer") {
    val customer =
      Customer(
        id = "CUST_000001",
        firstName = "John",
        lastName = "Doe",
        email = "john.doe@example.com",
        phone = "9999999999",
        gender = "male",
        dateOfBirth = LocalDate.of(1990, 5, 10),
        registrationDate = LocalDate.of(2023, 6, 15),
        customerStatus = "active",
        customerSegment = "standard",
        acquisitionChannel = "organic",
        acquisitionCampaign = "seo",
        preferredDevice = "mobile",
        preferredPaymentMethod = "upi"
      )

    val randomGenerator = new RandomGenerator(42L)

    val orderGenerator =
      new OrderGenerator(
        customer,
        randomGenerator
      )

    val order = orderGenerator.generate()

    assert(order.id == "ORD_000001")
    assert(order.customerId == customer.id)
    assert(!order.orderDate.isBefore(customer.registrationDate))
    assert(!order.orderDate.isAfter(LocalDate.of(2026, 1, 1)))
    assert(order.orderStatus == "completed")
    assert(order.totalAmount == BigDecimal("0.00"))
  }
}