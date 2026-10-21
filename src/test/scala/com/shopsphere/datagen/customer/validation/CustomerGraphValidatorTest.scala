
package com.shopsphere.datagen.customer.validation

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.customer.generator.{
  CustomerGraphGenerator,
  CustomerIdentityGenerator,
  CustomerGenerator
}
import com.shopsphere.datagen.customer.model.CustomerGraph
import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.geography.validation.GeographyValidator
import com.shopsphere.datagen.order.generator.OrderItemIdGenerator
import com.shopsphere.datagen.payment.generator.PaymentIdGenerator
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class CustomerGraphValidatorTest extends AnyFunSuite {

  private val seed = 42L
  private val asOfDate = LocalDate.of(2026, 1, 1)
  private val expectedItemsPerOrder = 2

  private val geographyReferenceData: GeographyReferenceData =
    new GeographyLoader().load()

  private val productReferenceData =
    new ProductReferenceData(new ProductLoader().load())

  private val customerConfig = {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile

    CustomerLoaderFactory
      .createCustomerConfigLoader()
      .loadConfiguration(config.getConfig(DataGenerationConstants.CUSTOMER))
  }

  private val graph: CustomerGraph = {
    new GeographyValidator(geographyReferenceData).validate()

    val random = new RandomGenerator(seed)
    val identityGenerator = new CustomerIdentityGenerator(random)
    val customer = new CustomerGenerator(
      customerConfig,
      random,
      identityGenerator
    ).generate()

    new CustomerGraphGenerator(
      customer,
      geographyReferenceData,
      productReferenceData,
      random,
      new OrderItemIdGenerator(),
      new PaymentIdGenerator(),
      asOfDate
    ).generate(
      addressCount = 2,
      orderCount = 2,
      orderItemCount = expectedItemsPerOrder
    )
  }

  private def validator =
    new CustomerGraphValidator(
      geographyReferenceData,
      productReferenceData,
      asOfDate
    )

  test("should accept a valid customer graph") {
    validator.validate(graph, expectedItemsPerOrder)
  }

  test("should reject an address linked to another customer") {
    val invalidAddress =
      graph.addresses.head.copy(customerId = "CUST999999")

    val invalidGraph =
      graph.copy(addresses = graph.addresses.updated(0, invalidAddress))

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject duplicate address IDs") {
    val duplicate =
      graph.addresses.updated(1, graph.addresses.head)

    assertThrows[IllegalArgumentException] {
      validator.validate(
        graph.copy(addresses = duplicate),
        expectedItemsPerOrder
      )
    }
  }

  test("should reject duplicate order IDs") {
    val duplicate =
      graph.orders.updated(1, graph.orders.head)

    assertThrows[IllegalArgumentException] {
      validator.validate(
        graph.copy(orders = duplicate),
        expectedItemsPerOrder
      )
    }
  }

  test("should reject an order item referencing an unknown order") {
    val invalidItem =
      graph.orderItems.head.copy(orderId = "ORD999999_CUST999999")

    val invalidGraph =
      graph.copy(
        orderItems = graph.orderItems.updated(0, invalidItem)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject an order item referencing an unknown product") {
    val invalidItem =
      graph.orderItems.head.copy(productId = "PROD999999")

    val invalidGraph =
      graph.copy(
        orderItems = graph.orderItems.updated(0, invalidItem)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject an inconsistent order-item total") {
    val item = graph.orderItems.head
    val invalidItem = item.copy(totalPrice = item.totalPrice + BigDecimal(1))

    val invalidGraph =
      graph.copy(
        orderItems = graph.orderItems.updated(0, invalidItem)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject an incorrect order total") {
    val order = graph.orders.head
    val invalidOrder =
      order.copy(totalAmount = order.totalAmount + BigDecimal(1))

    val invalidGraph =
      graph.copy(
        orders = graph.orders.updated(0, invalidOrder)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject a payment referencing an unknown order") {
    val payment = graph.payments.head
    val invalidPayment =
      payment.copy(orderId = "ORD999999_CUST999999")

    val invalidGraph =
      graph.copy(
        payments = graph.payments.updated(0, invalidPayment)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject a payment amount differing from its order total") {
    val payment = graph.payments.head
    val invalidPayment =
      payment.copy(amount = payment.amount + BigDecimal(1))

    val invalidGraph =
      graph.copy(
        payments = graph.payments.updated(0, invalidPayment)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject an order dated after the as-of date") {
    val order = graph.orders.head
    val invalidOrder =
      order.copy(orderDate = asOfDate.plusDays(1))

    val invalidGraph =
      graph.copy(
        orders = graph.orders.updated(0, invalidOrder)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject a payment dated after the as-of date") {
    val payment = graph.payments.head
    val invalidPayment =
      payment.copy(paymentDate = asOfDate.plusDays(1))

    val invalidGraph =
      graph.copy(
        payments = graph.payments.updated(0, invalidPayment)
      )

    assertThrows[IllegalArgumentException] {
      validator.validate(invalidGraph, expectedItemsPerOrder)
    }
  }

  test("should reject an incorrect number of items per order") {
    assertThrows[IllegalArgumentException] {
      validator.validate(graph, expectedOrderItemCount = 3)
    }
  }
}
