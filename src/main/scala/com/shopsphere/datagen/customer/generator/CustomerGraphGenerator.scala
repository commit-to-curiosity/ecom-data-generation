package com.shopsphere.datagen.customer.generator

import com.shopsphere.datagen.address.generator.AddressGenerator
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.customer.model.{Customer, CustomerGraph}
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.order.generator.{OrderGenerator, OrderItemGenerator}
import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.shopsphere.datagen.payment.generator.PaymentGenerator

class CustomerGraphGenerator(
                              customer: Customer,
                              geographyReferenceData: GeographyReferenceData,
                              productReferenceData: ProductReferenceData,
                              randomGenerator: RandomGenerator
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

    val addresses = (1 to addressCount).map { _ =>
      addressGenerator.generate()
    }

    val orderGenerator =
      new OrderGenerator(
        customer,
        randomGenerator
      )

    val generatedOrders =
      (1 to orderCount).map { _ =>

        val generatedOrder =
          orderGenerator.generate()

        val orderItemGenerator =
          new OrderItemGenerator(
            generatedOrder,
            productReferenceData,
            randomGenerator
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

        val payment =
          new PaymentGenerator(
            order,
            randomGenerator
          ).generate()

        (order, orderItems, payment)
      }

    CustomerGraph(
      customer = customer,
      addresses = addresses,
      orders = generatedOrders.map(_._1),
      orderItems = generatedOrders.flatMap(_._2),
      payments = generatedOrders.map(_._3)
    )
  }
}