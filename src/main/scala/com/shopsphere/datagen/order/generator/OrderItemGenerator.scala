
package com.shopsphere.datagen.order.generator

import com.shopsphere.datagen.common.distribution.{
  RandomGenerator,
  UniformDistribution,
  WeightedDistribution
}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.order.model.{Order, OrderItem}
import com.shopsphere.datagen.product.model.Product
import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.shopsphere.datagen.scenario.{
  ProductSelectionConfig,
  ProductSelectionStrategy
}

class OrderItemGenerator(
                          order: Order,
                          productReferenceData: ProductReferenceData,
                          randomGenerator: RandomGenerator,
                          orderItemIdGenerator: OrderItemIdGenerator,
                          productSelection: ProductSelectionConfig
                        ) extends Generator[OrderItem] {

  private val products = productReferenceData.products

  require(products.nonEmpty, "Product reference data must not be empty")

  private val productSelector: () => Product =
    productSelection.strategy match {
      case ProductSelectionStrategy.Uniform =>
        val distribution =
          new UniformDistribution[Product](products, randomGenerator)

        () => distribution.sample()

      case ProductSelectionStrategy.Weighted =>
        val unknownProductIds =
          productSelection.weights.keySet -- productReferenceData.productsById.keySet

        require(
          unknownProductIds.isEmpty,
          s"Unknown product IDs in scenario weights: ${unknownProductIds.toSeq.sorted.mkString(", ")}"
        )

        val weightedProducts =
          productSelection.weights.toSeq.flatMap {
            case (productId, weight) =>
              productReferenceData.productsById.get(productId).map(_ -> weight)
          }.sortBy(_._1.id)

        val distribution =
          new WeightedDistribution[Product](weightedProducts, randomGenerator)

        () => distribution.sample()
    }

  override def generate(): OrderItem = {
    val product = productSelector()

    val quantity =
      new UniformDistribution[Int](
        1 to 5,
        randomGenerator
      ).sample()

    val unitPrice = product.price
    val totalPrice = unitPrice * quantity

    OrderItem(
      id = generateOrderItemId(),
      orderId = order.id,
      productId = product.id,
      quantity = quantity,
      unitPrice = unitPrice,
      totalPrice = totalPrice
    )
  }

  private def generateOrderItemId(): String =
    orderItemIdGenerator.generate(order.id)
}
