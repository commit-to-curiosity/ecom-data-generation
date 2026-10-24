
package com.shopsphere.datagen.common.parallel

import com.shopsphere.datagen.common.config.{
  ConfigReaderFactory,
  DataGenerationConstants
}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.geography.validation.GeographyValidator
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import org.scalatest.funsuite.AnyFunSuite

class CustomerChunkGeneratorTest extends AnyFunSuite {

  private val customerConfig: CustomerConfig = {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile

    CustomerLoaderFactory
      .createCustomerConfigLoader()
      .loadConfiguration(
        config.getConfig(DataGenerationConstants.CUSTOMER)
      )
  }

  private val geographyReferenceData = new GeographyLoader().load()

  new GeographyValidator(geographyReferenceData).validate()

  private val products = new ProductLoader().load()
  private val productReferenceData = new ProductReferenceData(products)

  private def createGenerator(): CustomerChunkGenerator =
    new CustomerChunkGenerator(
      customerConfig,
      geographyReferenceData,
      productReferenceData,
      addressCount = 2,
      orderCount = 2,
      orderItemCountPerOrder = 2
    )

  test("generates customer graphs in ascending global ordinal order") {
    val chunk = GenerationChunk(
      index = 1L,
      firstCustomerOrdinal = 10001L,
      customerCount = 3L
    )

    val graphs = createGenerator().generate(chunk, masterSeed = 42L)

    assert(graphs.map(_.customer.id) ==
      Seq("CUST010001", "CUST010002", "CUST010003"))
    assert(graphs.forall(_.addresses.size == 2))
    assert(graphs.forall(_.orders.size == 2))
    assert(graphs.forall(_.orderItems.size == 4))
  }

  test("same chunk and master seed produce identical graphs") {
    val chunk = GenerationChunk(
      index = 0L,
      firstCustomerOrdinal = 1L,
      customerCount = 3L
    )

    val first = createGenerator().generate(chunk, masterSeed = 42L)
    val second = createGenerator().generate(chunk, masterSeed = 42L)

    assert(first == second)
  }

  test("different chunk indices use different generated streams") {
    val firstChunk = GenerationChunk(0L, 1L, 3L)
    val secondChunk = GenerationChunk(1L, 4L, 3L)

    val first =
      createGenerator().generate(firstChunk, masterSeed = 42L)
    val second =
      createGenerator().generate(secondChunk, masterSeed = 42L)

    assert(first.map(_.customer.id) ==
      Seq("CUST000001", "CUST000002", "CUST000003"))
    assert(second.map(_.customer.id) ==
      Seq("CUST000004", "CUST000005", "CUST000006"))
    assert(first != second)
  }

  test("zero-entity counts produce valid graphs without those entities") {
    val generator =
      new CustomerChunkGenerator(
        customerConfig,
        geographyReferenceData,
        productReferenceData,
        addressCount = 0,
        orderCount = 0,
        orderItemCountPerOrder = 0
      )

    val graphs = generator.generate(
      GenerationChunk(0L, 1L, 1L),
      masterSeed = 42L
    )

    assert(graphs.size == 1)
    assert(graphs.head.addresses.isEmpty)
    assert(graphs.head.orders.isEmpty)
    assert(graphs.head.orderItems.isEmpty)
    assert(graphs.head.payments.isEmpty)
  }
}
