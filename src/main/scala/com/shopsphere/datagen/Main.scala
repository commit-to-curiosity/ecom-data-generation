package com.shopsphere.datagen

import com.shopsphere.datagen.address.model.AddressExport
import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.common.output.{JsonLinesWriter, RunOutputPublisher}
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.generator.{CustomerGenerator, CustomerGraphGenerator, CustomerIdentityGenerator}
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import com.shopsphere.datagen.customer.validation.CustomerGraphValidator
import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.geography.validation.GeographyValidator
import com.shopsphere.datagen.order.generator.OrderItemIdGenerator
import com.shopsphere.datagen.payment.generator.PaymentIdGenerator
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import org.apache.logging.log4j.LogManager

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object Main {

  private val logger = LogManager.getLogger(getClass)

  private val AddressCount = 2
  private val OrderCount = 2
  private val OrderItemCountPerOrder = 2

  private val DefaultOutputRoot: Path = Paths.get("data", "raw")

  def main(args: Array[String]): Unit = {
    val outputRoot =
      args.headOption
        .map(Paths.get(_))
        .getOrElse(DefaultOutputRoot)

    logger.info("ShopSphere E-Commerce Data Generator started")

    val configFormat = ConfigFileFormat.HOCON
    logger.info(s"Configuration format: ${configFormat.name}")

    val configReader = ConfigReaderFactory.createConfigReader(configFormat)
    val config = configReader.readConfigFile

    val customerConfigLoader = CustomerLoaderFactory.createCustomerConfigLoader()
    val customerConfig: CustomerConfig =
      customerConfigLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.CUSTOMER)
      )

    logger.info("Customer configuration loaded successfully")

    val randomSeed = 42L
    logger.info(s"Random seed: $randomSeed")

    val randomGenerator = new RandomGenerator(randomSeed)
    val identityGenerator = new CustomerIdentityGenerator(randomGenerator)

    val customerGenerator = new CustomerGenerator(
      customerConfig,
      randomGenerator,
      identityGenerator
    )

    val customer = customerGenerator.generate()
    logger.info(s"Customer generated: ${customer.id}")

    val geographyReferenceData = new GeographyLoader().load()
    new GeographyValidator(geographyReferenceData).validate()
    logger.info("Geography reference data loaded and validated")

    val products = new ProductLoader().load()
    require(products.nonEmpty, "Product reference data must not be empty")

    val productReferenceData = new ProductReferenceData(products)
    logger.info(s"Product reference data loaded: ${products.size} products")

    val graphGenerator = new CustomerGraphGenerator(
      customer,
      geographyReferenceData,
      productReferenceData,
      randomGenerator,
      new OrderItemIdGenerator(),
      new PaymentIdGenerator(),
      customerConfig.lifecycle.asOfDate
    )

    val graph = graphGenerator.generate(
      addressCount = AddressCount,
      orderCount = OrderCount,
      orderItemCount = OrderItemCountPerOrder
    )

    new CustomerGraphValidator(
      geographyReferenceData,
      productReferenceData,
      customerConfig.lifecycle.asOfDate
    ).validate(
      graph,
      expectedOrderItemCount = OrderItemCountPerOrder
    )

    logger.info("Customer graph validation passed")

    logger.info(
      s"""Customer graph generated:
         |customerId=${graph.customer.id}
         |addresses=${graph.addresses.size}
         |orders=${graph.orders.size}
         |orderItems=${graph.orderItems.size}
         |payments=${graph.payments.size}""".stripMargin
    )

    val timestamp =
      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val runId = s"${timestamp}_${UUID.randomUUID().toString.take(8)}"

    val jsonLinesWriter = new JsonLinesWriter(JsonMapper.mapper)
    val publisher = new RunOutputPublisher()

    val addressExports: Seq[AddressExport] =
      graph.addresses.map { address =>
        val location = geographyReferenceData
          .resolveBuilding(address.buildingId)
          .getOrElse {
            throw new IllegalStateException(
              s"Unable to resolve building '${address.buildingId}' for address '${address.id}'"
            )
          }

        AddressExport(
          id = address.id,
          customerId = address.customerId,
          buildingId = address.buildingId,
          unitNumber = address.unitNumber,
          postalCode = address.postalCode,
          buildingName = location.building.name,
          societyName = location.society.name,
          roadName = location.road.name,
          areaName = location.area.name,
          cityName = location.city.name,
          stateName = location.state.name,
          countryName = location.country.name
        )
      }

    val publishedRun = publisher.publish(outputRoot, runId) { temporaryDirectory =>
      Map(
        "customers.jsonl" ->
          jsonLinesWriter.write(
            temporaryDirectory.resolve("customers.jsonl"),
            List(graph.customer)
          ),
        "addresses.jsonl" ->
          jsonLinesWriter.write(
            temporaryDirectory.resolve("addresses.jsonl"),
            addressExports
          ),
        "orders.jsonl" ->
          jsonLinesWriter.write(
            temporaryDirectory.resolve("orders.jsonl"),
            graph.orders
          ),
        "order_items.jsonl" ->
          jsonLinesWriter.write(
            temporaryDirectory.resolve("order_items.jsonl"),
            graph.orderItems
          ),
        "payments.jsonl" ->
          jsonLinesWriter.write(
            temporaryDirectory.resolve("payments.jsonl"),
            graph.payments
          )
      )
    }

    logger.info(
      s"""Output published successfully:
         |directory=${publishedRun.directory}
         |${publishedRun.recordCounts.toSeq.sortBy(_._1).map {
        case (file, count) => s"$file=$count"
      }.mkString("\n")}""".stripMargin
    )
  }
}
