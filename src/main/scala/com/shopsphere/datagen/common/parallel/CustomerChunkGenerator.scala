
package com.shopsphere.datagen.common.parallel

import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.generator.{
  CustomerGenerator,
  CustomerGraphGenerator,
  CustomerIdentityGenerator
}
import com.shopsphere.datagen.customer.model.CustomerGraph
import com.shopsphere.datagen.customer.validation.CustomerGraphValidator
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.order.generator.OrderItemIdGenerator
import com.shopsphere.datagen.payment.generator.PaymentIdGenerator
import com.shopsphere.datagen.product.reference.ProductReferenceData

/**
 * Generates complete customer graphs for one deterministic chunk.
 *
 * A chunk owns one random stream. Each customer graph receives fresh
 * ID generators so its mutable ID state is isolated from other graphs.
 */
class CustomerChunkGenerator(
                              customerConfig: CustomerConfig,
                              geographyReferenceData: GeographyReferenceData,
                              productReferenceData: ProductReferenceData,
                              addressCount: Int,
                              orderCount: Int,
                              orderItemCountPerOrder: Int
                            ) {

  require(addressCount >= 0, "Address count must not be negative")
  require(orderCount >= 0, "Order count must not be negative")
  require(
    orderItemCountPerOrder >= 0,
    "Order-item count must not be negative"
  )

  private val asOfDate = customerConfig.lifecycle.asOfDate

  private val graphValidator =
    new CustomerGraphValidator(
      geographyReferenceData,
      productReferenceData,
      asOfDate
    )

  def generate(
                chunk: GenerationChunk,
                masterSeed: Long
              ): Seq[CustomerGraph] = {
    require(chunk != null, "Generation chunk must not be null")

    val chunkSeed =
      ChunkSeedDerivation.derive(masterSeed, chunk.index)

    val randomGenerator = new RandomGenerator(chunkSeed)
    val identityGenerator =
      new CustomerIdentityGenerator(randomGenerator)

    val customerGenerator =
      new CustomerGenerator(
        customerConfig,
        randomGenerator,
        identityGenerator
      )

    val graphs = Vector.newBuilder[CustomerGraph]
    var offset = 0L

    while (offset < chunk.customerCount) {
      val ordinal = Math.addExact(chunk.firstCustomerOrdinal, offset)
      val customer = customerGenerator.generate(ordinal)

      val graphGenerator =
        new CustomerGraphGenerator(
          customer,
          geographyReferenceData,
          productReferenceData,
          randomGenerator,
          new OrderItemIdGenerator(),
          new PaymentIdGenerator(),
          asOfDate
        )

      val graph =
        graphGenerator.generate(
          addressCount = addressCount,
          orderCount = orderCount,
          orderItemCount = orderItemCountPerOrder
        )

      graphValidator.validate(
        graph,
        expectedOrderItemCount = orderItemCountPerOrder
      )

      graphs += graph
      offset += 1L
    }

    graphs.result()
  }
}
