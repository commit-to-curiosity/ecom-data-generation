package com.shopsphere.datagen.order

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.order.generator.{OrderGenerator, OrderItemGenerator, OrderItemIdGenerator}
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.shopsphere.datagen.customer.model.Customer
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class OrderItemGeneratorTest extends AnyFunSuite {

  test("should generate a valid order item for an order and product") {
    val customer =
      Customer(
        id = "CUST000001",
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

    val order =
      new OrderGenerator(
        customer,
        randomGenerator
      ).generate()

    val products =
      new ProductLoader().load()

    val productReferenceData =
      new ProductReferenceData(products)

    val orderItem =
      new OrderItemGenerator(
        order,
        productReferenceData,
        randomGenerator,
        new OrderItemIdGenerator()
      ).generate()

    val product =
      productReferenceData.findProduct(orderItem.productId).get

    assert(orderItem.id == s"ITEM000001_${order.id}")
    assert(orderItem.orderId == order.id)
    assert(orderItem.quantity >= 1)
    assert(orderItem.quantity <= 5)
    assert(orderItem.unitPrice == product.price)
    assert(orderItem.totalPrice == orderItem.unitPrice * orderItem.quantity)
  }

  test("OrderItemIdGenerator should generate unique sequential IDs") {
    val idGenerator = new OrderItemIdGenerator()

    val orderId1 = "ORD000001_CUST000001"
    val orderId2 = "ORD000002_CUST000001"

    val firstId = idGenerator.generate(orderId1)
    val secondId = idGenerator.generate(orderId1)
    val thirdId = idGenerator.generate(orderId1)
    val firstIdForSecondOrder = idGenerator.generate(orderId2)

    assert(firstId == s"ITEM000001_${orderId1}")
    assert(secondId == s"ITEM000002_${orderId1}")
    assert(thirdId == s"ITEM000003_${orderId1}")
    assert(firstIdForSecondOrder == s"ITEM000001_${orderId2}")
  }
}