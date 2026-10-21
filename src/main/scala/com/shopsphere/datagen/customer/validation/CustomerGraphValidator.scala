
package com.shopsphere.datagen.customer.validation

import com.shopsphere.datagen.customer.model.CustomerGraph
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.product.reference.ProductReferenceData

import java.time.LocalDate

class CustomerGraphValidator(
                              geographyReferenceData: GeographyReferenceData,
                              productReferenceData: ProductReferenceData,
                              asOfDate: LocalDate
                            ) {

  def validate(
                graph: CustomerGraph,
                expectedOrderItemCount: Int
              ): Unit = {
    require(graph != null, "Customer graph must not be null")
    require(expectedOrderItemCount >= 0,
      "Expected order-item count must not be negative")

    validateCustomer(graph)
    validateAddresses(graph)
    validateOrders(graph)
    validateOrderItems(graph, expectedOrderItemCount)
    validatePayments(graph)
  }

  private def validateCustomer(graph: CustomerGraph): Unit = {
    require(graph.customer != null, "Customer must not be null")
    require(
      graph.customer.id.matches("""CUST\d{6,}"""),
      s"Invalid customer ID: ${graph.customer.id}"
    )
    require(
      !graph.customer.registrationDate.isAfter(asOfDate),
      "Customer registration date must not be after the as-of date"
    )
  }

  private def validateAddresses(graph: CustomerGraph): Unit = {
    val customerId = graph.customer.id

    validateUniqueIds("address", graph.addresses.map(_.id))

    graph.addresses.foreach { address =>
      require(
        address.customerId == customerId,
        s"Address '${address.id}' references an unexpected customer"
      )
      require(
        address.id.matches(
          s"""ADDR\\d{6,}_${java.util.regex.Pattern.quote(customerId)}"""
        ),
        s"Invalid address ID: ${address.id}"
      )
      require(
        geographyReferenceData.buildingsById.contains(address.buildingId),
        s"Address '${address.id}' references unknown building '${address.buildingId}'"
      )
      require(
        geographyReferenceData.postalCodesByCode.contains(address.postalCode),
        s"Address '${address.id}' references unknown postal code '${address.postalCode}'"
      )
    }
  }

  private def validateOrders(graph: CustomerGraph): Unit = {
    val customerId = graph.customer.id

    validateUniqueIds("order", graph.orders.map(_.id))

    graph.orders.foreach { order =>
      require(
        order.customerId == customerId,
        s"Order '${order.id}' references an unexpected customer"
      )
      require(
        order.id.matches(
          s"""ORD\\d{6,}_${java.util.regex.Pattern.quote(customerId)}"""
        ),
        s"Invalid order ID: ${order.id}"
      )
      require(
        !order.orderDate.isBefore(graph.customer.registrationDate),
        s"Order '${order.id}' predates customer registration"
      )
      require(
        !order.orderDate.isAfter(asOfDate),
        s"Order '${order.id}' is after the as-of date"
      )
    }
  }

  private def validateOrderItems(
                                  graph: CustomerGraph,
                                  expectedOrderItemCount: Int
                                ): Unit = {
    validateUniqueIds("order item", graph.orderItems.map(_.id))

    val ordersById = graph.orders.map(order => order.id -> order).toMap

    graph.orderItems.foreach { item =>
      require(
        ordersById.contains(item.orderId),
        s"Order item '${item.id}' references unknown order '${item.orderId}'"
      )
      require(item.quantity > 0,
        s"Order item '${item.id}' must have a positive quantity")
      require(item.unitPrice >= BigDecimal(0),
        s"Order item '${item.id}' has a negative unit price")
      require(
        item.totalPrice == item.unitPrice * item.quantity,
        s"Order item '${item.id}' has an inconsistent total price"
      )
      require(
        productReferenceData.productsById.contains(item.productId),
        s"Order item '${item.id}' references unknown product '${item.productId}'"
      )

      val expectedId =
        s"ITEM${item.id.drop(4).takeWhile(_.isDigit)}_${item.orderId}"

      require(
        item.id == expectedId,
        s"Order item '${item.id}' does not follow the expected identifier format"
      )
    }

    graph.orders.foreach { order =>
      val items = graph.orderItems.filter(_.orderId == order.id)

      require(
        items.size == expectedOrderItemCount,
        s"Order '${order.id}' has ${items.size} items; expected $expectedOrderItemCount"
      )

      val calculatedTotal = items.map(_.totalPrice).sum

      require(
        order.totalAmount == calculatedTotal,
        s"Order '${order.id}' total does not equal the sum of its item totals"
      )
    }
  }

  private def validatePayments(graph: CustomerGraph): Unit = {
    validateUniqueIds("payment", graph.payments.map(_.id))

    val ordersById = graph.orders.map(order => order.id -> order).toMap

    graph.payments.foreach { payment =>
      val order = ordersById.getOrElse(
        payment.orderId,
        throw new IllegalArgumentException(
          s"Payment '${payment.id}' references unknown order '${payment.orderId}'"
        )
      )

      require(
        payment.id.matches(
          s"""PAY\\d{6,}_${java.util.regex.Pattern.quote(order.id)}"""
        ),
        s"Invalid payment ID: ${payment.id}"
      )
      require(
        payment.amount == order.totalAmount,
        s"Payment '${payment.id}' amount does not equal its order total"
      )
      require(
        !payment.paymentDate.isBefore(order.orderDate),
        s"Payment '${payment.id}' predates its order"
      )
      require(
        !payment.paymentDate.isAfter(asOfDate),
        s"Payment '${payment.id}' is after the as-of date"
      )
    }
  }

  private def validateUniqueIds(
                                 entityName: String,
                                 ids: Seq[String]
                               ): Unit = {
    val duplicates = ids
      .groupBy(identity)
      .collect {
        case (id, occurrences) if occurrences.size > 1 => id
      }

    require(
      duplicates.isEmpty,
      s"Duplicate $entityName IDs found: ${duplicates.mkString(", ")}"
    )
  }
}
