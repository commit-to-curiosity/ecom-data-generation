
package com.shopsphere.datagen.common.parallel

import com.shopsphere.datagen.address.model.AddressExport
import com.shopsphere.datagen.common.config.{
  ConfigReaderFactory,
  DataGenerationConstants
}
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.common.output.{
  JsonLinesStreamWriter,
  RunOutputPublisher
}
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.geography.validation.GeographyValidator
import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.shopsphere.datagen.scenario.GenerationScenarioLoader
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

class ParallelGenerationReproducibilityTest extends AnyFunSuite {

  private val MasterSeed = 42L
  private val TotalCustomers = 53L
  private val ChunkSize = 10L
  private val AddressCount = 2

  private val OutputFiles = Seq(
    "customers.jsonl",
    "addresses.jsonl",
    "orders.jsonl",
    "order_items.jsonl",
    "payments.jsonl"
  )

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

  private val geographyReferenceData =
    new GeographyLoader().load()

  new GeographyValidator(geographyReferenceData).validate()

  private val products =
    new ProductLoader().load()

  private val productReferenceData =
    new ProductReferenceData(products)

  private val scenario =
    new GenerationScenarioLoader(productReferenceData).loadDefault()

  private val chunkGenerator =
    new CustomerChunkGenerator(
      customerConfig = customerConfig,
      geographyReferenceData = geographyReferenceData,
      productReferenceData = productReferenceData,
      addressCount = AddressCount,
      scenario = scenario
    )

  private def deleteRecursively(root: Path): Unit = {
    if (Files.exists(root)) {
      val paths = Files.walk(root)

      try {
        paths.iterator().asScala.toList
          .sortBy(_.getNameCount)
          .reverse
          .foreach(Files.deleteIfExists)
      } finally {
        paths.close()
      }
    }
  }

  private def generateRun(
                           outputRoot: Path,
                           parallelism: Int,
                           maxInFlight: Int,
                           runId: String
                         ): Path = {
    val publisher = new RunOutputPublisher()

    val publishedRun = publisher.publish(outputRoot, runId) {
      temporaryDirectory =>
        val writer =
          new JsonLinesStreamWriter(
            JsonMapper.mapper,
            temporaryDirectory
          )

        try {
          // Create all five files, including for empty entity collections.
          OutputFiles.foreach { fileName =>
            writer.write(fileName, Seq.empty[Any])
          }

          val chunks =
            GenerationChunkPlanner.plan(
              totalCustomers = TotalCustomers,
              chunkSize = ChunkSize
            )

          val executor =
            new ParallelChunkExecutor(
              parallelism = parallelism,
              maxInFlight = maxInFlight
            )

          executor.execute(chunks) { chunk =>
            chunkGenerator.generate(chunk, MasterSeed)
          } { (_, graphs) =>

            val customers =
              graphs.map(_.customer)

            val addresses: Seq[AddressExport] =
              graphs.flatMap { graph =>
                graph.addresses.map { address =>
                  val location =
                    geographyReferenceData
                      .resolveBuilding(address.buildingId)
                      .getOrElse {
                        throw new IllegalStateException(
                          s"Unable to resolve building " +
                            s"'${address.buildingId}' for address " +
                            s"'${address.id}'"
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

            val orders =
              graphs.flatMap(_.orders)

            val orderItems =
              graphs.flatMap(_.orderItems)

            val payments =
              graphs.flatMap(_.payments)

            writer.write("customers.jsonl", customers)
            writer.write("addresses.jsonl", addresses)
            writer.write("orders.jsonl", orders)
            writer.write("order_items.jsonl", orderItems)
            writer.write("payments.jsonl", payments)
          }

          writer.recordCounts
        } finally {
          writer.close()
        }
    }

    publishedRun.directory
  }

  private def readBytes(
                         runDirectory: Path,
                         fileName: String
                       ): Vector[Byte] =
    Files.readAllBytes(runDirectory.resolve(fileName)).toVector

  test(
    "generated JSONL output must be identical across worker configurations"
  ) {
    val singleWorkerRoot =
      Files.createTempDirectory("shopsphere-single-worker")

    val parallelWorkerRoot =
      Files.createTempDirectory("shopsphere-parallel-workers")

    try {
      val singleWorkerRun =
        generateRun(
          outputRoot = singleWorkerRoot,
          parallelism = 1,
          maxInFlight = 8,
          runId = "single_worker"
        )

      val parallelWorkerRun =
        generateRun(
          outputRoot = parallelWorkerRoot,
          parallelism = 4,
          maxInFlight = 8,
          runId = "parallel_workers"
        )

      OutputFiles.foreach { fileName =>
        val singleWorkerBytes =
          readBytes(singleWorkerRun, fileName)

        val parallelWorkerBytes =
          readBytes(parallelWorkerRun, fileName)

        assert(
          singleWorkerBytes == parallelWorkerBytes,
          s"Output differs across worker configurations: $fileName"
        )
      }
    } finally {
      deleteRecursively(singleWorkerRoot)
      deleteRecursively(parallelWorkerRoot)
    }
  }
}
