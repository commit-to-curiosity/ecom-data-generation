
package com.shopsphere.datagen

import com.shopsphere.datagen.address.model.AddressExport
import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.common.output.{
  JsonLinesStreamWriter,
  RunOutputPublisher
}
import com.shopsphere.datagen.common.parallel.{
  CustomerChunkGenerator,
  GenerationChunkPlanner,
  GenerationRunConfig,
  ParallelChunkExecutor
}
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.geography.validation.GeographyValidator
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
      args.headOption.map(Paths.get(_)).getOrElse(DefaultOutputRoot)

    val runConfig = GenerationRunConfig()

    logger.info("ShopSphere E-Commerce Data Generator started")
    logger.info(s"Master seed: ${runConfig.masterSeed}")
    logger.info(s"Total customers: ${runConfig.totalCustomers}")
    logger.info(s"Chunk size: ${runConfig.chunkSize}")
    logger.info(s"Parallelism: ${runConfig.parallelism}")
    logger.info(s"Max in-flight chunks: ${runConfig.maxInFlight}")

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

    val geographyReferenceData = new GeographyLoader().load()
    new GeographyValidator(geographyReferenceData).validate()
    logger.info("Geography reference data loaded and validated")

    val products = new ProductLoader().load()
    require(products.nonEmpty, "Product reference data must not be empty")

    val productReferenceData = new ProductReferenceData(products)
    logger.info(s"Product reference data loaded: ${products.size} products")

    val chunkGenerator = new CustomerChunkGenerator(
      customerConfig = customerConfig,
      geographyReferenceData = geographyReferenceData,
      productReferenceData = productReferenceData,
      addressCount = AddressCount,
      orderCount = OrderCount,
      orderItemCountPerOrder = OrderItemCountPerOrder
    )

    val timestamp =
      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val runId = s"${timestamp}_${UUID.randomUUID().toString.take(8)}"

    val publisher = new RunOutputPublisher()

    val publishedRun = publisher.publish(outputRoot, runId) {
      temporaryDirectory =>
        val streamWriter =
          new JsonLinesStreamWriter(JsonMapper.mapper, temporaryDirectory)

        try {
          // Create all five files even when totalCustomers is zero.
          streamWriter.write("customers.jsonl", Seq.empty[Any])
          streamWriter.write("addresses.jsonl", Seq.empty[Any])
          streamWriter.write("orders.jsonl", Seq.empty[Any])
          streamWriter.write("order_items.jsonl", Seq.empty[Any])
          streamWriter.write("payments.jsonl", Seq.empty[Any])

          val chunks = GenerationChunkPlanner.plan(
            totalCustomers = runConfig.totalCustomers,
            chunkSize = runConfig.chunkSize
          )

          val executor = new ParallelChunkExecutor(
            parallelism = runConfig.parallelism,
            maxInFlight = runConfig.maxInFlight
          )

          executor.execute(chunks) { chunk =>
            chunkGenerator.generate(chunk, runConfig.masterSeed)
          } { (chunk, graphs) =>
            val customers = graphs.map(_.customer)

            val addresses: Seq[AddressExport] =
              graphs.flatMap { graph =>
                graph.addresses.map { address =>
                  val location = geographyReferenceData
                    .resolveBuilding(address.buildingId)
                    .getOrElse {
                      throw new IllegalStateException(
                        s"Unable to resolve building '${address.buildingId}' " +
                          s"for address '${address.id}'"
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
              }

            val orders = graphs.flatMap(_.orders)
            val orderItems = graphs.flatMap(_.orderItems)
            val payments = graphs.flatMap(_.payments)

            streamWriter.write("customers.jsonl", customers)
            streamWriter.write("addresses.jsonl", addresses)
            streamWriter.write("orders.jsonl", orders)
            streamWriter.write("order_items.jsonl", orderItems)
            streamWriter.write("payments.jsonl", payments)

            logger.info(
              s"Chunk ${chunk.index} completed: " +
                s"firstCustomerOrdinal=${chunk.firstCustomerOrdinal}, " +
                s"customers=${graphs.size}"
            )
          }

          streamWriter.recordCounts
        } finally {
          streamWriter.close()
        }
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
