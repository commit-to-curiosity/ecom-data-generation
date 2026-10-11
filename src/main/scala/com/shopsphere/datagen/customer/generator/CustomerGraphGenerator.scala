package com.shopsphere.datagen.customer.generator

import com.shopsphere.datagen.address.generator.AddressGenerator
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.customer.model.{Customer, CustomerGraph}
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.order.generator.{OrderGenerator, OrderItemGenerator, OrderItemIdGenerator}
import com.shopsphere.datagen.payment.generator.{PaymentAttemptOutcomeGenerator, PaymentGenerator, PaymentIdGenerator}
import com.shopsphere.datagen.product.reference.ProductReferenceData

class CustomerGraphGenerator(
                              customer: Customer,
                              geographyReferenceData: GeographyReferenceData,
                              productReferenceData: ProductReferenceData,
                              randomGenerator: RandomGenerator,
                              orderItemIdGenerator: OrderItemIdGenerator
                            ) {

  def generate(
                addressCount: Int,
                orderCount: Int,
                orderItemCount: Int
              ): CustomerGraph = {

    val addressGenerator =
      new AddressGenerator(
        customer.id,
        geographyReferenceData,
        randomGenerator
      )

    val addresses =
      (1 to addressCount).map { _ =>
        addressGenerator.generate()
      }

    val orderGenerator =
      new OrderGenerator(
        customer,
        randomGenerator
      )

    val paymentAttemptOutcomeGenerator =
      new PaymentAttemptOutcomeGenerator(
        randomGenerator
      )

    val orderItemIdGenerator = new OrderItemIdGenerator()

    val generatedOrders =
      (1 to orderCount).map { _ =>

        val generatedOrder =
          orderGenerator.generate()

        val orderItemGenerator =
          new OrderItemGenerator(
            generatedOrder,
            productReferenceData,
            randomGenerator,
            orderItemIdGenerator
          )

        val orderItems =
          (1 to orderItemCount).map { _ =>
            orderItemGenerator.generate()
          }

        val totalAmount =
          orderItems.map(_.totalPrice).sum

        val order =
          generatedOrder.copy(
            totalAmount = totalAmount
          )

        val paymentAttemptOutcome =
          paymentAttemptOutcomeGenerator.generate()

        val paymentIdGenerator = new PaymentIdGenerator()

        val paymentGenerator =
          new PaymentGenerator(
            order,
            randomGenerator,
            paymentIdGenerator
          )

        val payments =
          paymentAttemptOutcome.paymentStatuses.map { paymentStatus =>
            paymentGenerator.generate(paymentStatus)
          }

        (order, orderItems, payments)
      }

    CustomerGraph(
      customer = customer,
      addresses = addresses,
      orders = generatedOrders.map(_._1),
      orderItems = generatedOrders.flatMap(_._2),
      payments = generatedOrders.flatMap(_._3)
    )
  }
}