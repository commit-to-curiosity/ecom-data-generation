package com.shopsphere.datagen.customer.generator

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.{PaymentAttemptOutcome, PaymentStatus}
import com.shopsphere.datagen.customer.model.Customer
import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.order.generator.OrderItemIdGenerator
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class CustomerGraphGeneratorTest extends AnyFunSuite {

  test("CustomerGraphGenerator should generate a connected customer graph") {

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

    val geographyReferenceData =
      new GeographyLoader().load()

    val productReferenceData =
      new ProductReferenceData(
        new ProductLoader().load()
      )

    val graph =
      new CustomerGraphGenerator(
        customer,
        geographyReferenceData,
        productReferenceData,
        randomGenerator,
        new OrderItemIdGenerator()
      ).generate(
        addressCount = 2,
        orderCount = 2,
        orderItemCount = 2
      )

    assert(graph.customer.id == customer.id)

    assert(graph.addresses.size == 2)
    assert(graph.orders.size == 2)
    assert(graph.orderItems.size == 4)
    assert(graph.payments.nonEmpty)

    assert(
      graph.addresses.forall(_.customerId == customer.id)
    )

    val order = graph.orders.head
    val payment = graph.payments.head

    assert(order.customerId == customer.id)

    assert(
      graph.orderItems.count(_.orderId == order.id) == 2
    )

    assert(payment.orderId == order.id)

    graph.orders.foreach { order =>

      val orderItems =
        graph.orderItems.filter(_.orderId == order.id)

      val calculatedTotal =
        orderItems.map(_.totalPrice).sum

      assert(order.totalAmount == calculatedTotal)

      val payments =
        graph.payments.filter(_.orderId == order.id)

      assert(payments.nonEmpty)

      val paymentStatuses =
        payments.map(_.paymentStatus)

      assert(
        PaymentAttemptOutcome.all.exists { outcome =>
          outcome.paymentStatuses.map(_.name) == paymentStatuses
        }
      )

      payments.foreach { payment =>
        assert(payment.amount == order.totalAmount)
        assert(!payment.paymentDate.isBefore(order.orderDate))
        assert(!payment.paymentDate.isAfter(LocalDate.of(2026, 1, 1)))
      }
    }
  }
}