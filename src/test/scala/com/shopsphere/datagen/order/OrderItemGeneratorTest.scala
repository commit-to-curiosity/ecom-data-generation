package com.shopsphere.datagen.order

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.order.generator.{OrderGenerator, OrderItemGenerator}
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.shopsphere.datagen.customer.model.Customer
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class OrderItemGeneratorTest extends AnyFunSuite {

  test("should generate a valid order item for an order and product") {
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
        randomGenerator
      ).generate()

    val product =
      productReferenceData.findProduct(orderItem.productId).get

    assert(orderItem.id == "ITEM_000001")
    assert(orderItem.orderId == order.id)
    assert(orderItem.quantity >= 1)
    assert(orderItem.quantity <= 5)
    assert(orderItem.unitPrice == product.price)
    assert(orderItem.totalPrice == orderItem.unitPrice * orderItem.quantity)
  }
}