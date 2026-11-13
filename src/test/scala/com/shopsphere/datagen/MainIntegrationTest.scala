
package com.shopsphere.datagen

import com.fasterxml.jackson.databind.JsonNode
import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.common.parallel.GenerationRunConfig
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

class MainIntegrationTest extends AnyFunSuite {

  private val ExpectedFiles = Set(
    "customers.jsonl",
    "addresses.jsonl",
    "orders.jsonl",
    "order_items.jsonl",
    "payments.jsonl"
  )

  private def withOutputRoot[A](prefix: String)(
    test: Path => A
  ): A = {
    val outputRoot = Files.createTempDirectory(prefix)

    try {
      test(outputRoot)
    } finally {
      val paths = Files.walk(outputRoot)

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

  private def publishedRunDirectory(outputRoot: Path): Path = {
    val paths = Files.list(outputRoot)

    val runDirectories =
      try {
        paths.iterator().asScala
          .filter(Files.isDirectory(_))
          .toList
      } finally {
        paths.close()
      }

    assert(
      runDirectories.size == 1,
      s"Expected exactly one published run directory, found ${runDirectories.size}"
    )

    runDirectories.head
  }

  private def readRecords(
                           runDirectory: Path,
                           fileName: String
                         ): List[JsonNode] = {
    val lines = Files.readAllLines(
      runDirectory.resolve(fileName),
      StandardCharsets.UTF_8
    )

    lines.asScala.toList.map { line =>
      val json = JsonMapper.mapper.readTree(line)

      assert(
        json.isObject,
        s"Expected a JSON object in $fileName"
      )

      json
    }
  }

  private def assertExpectedFiles(runDirectory: Path): Unit = {
    val paths = Files.list(runDirectory)

    val actualFiles =
      try {
        paths.iterator().asScala
          .map(_.getFileName.toString)
          .toSet
      } finally {
        paths.close()
      }

    assert(actualFiles == ExpectedFiles)
  }

  private def runWithConfig(
                             outputRoot: Path,
                             runConfig: GenerationRunConfig
                           ): Path = {
    Main.run(
      Array(outputRoot.toString),
      runConfig
    )

    publishedRunDirectory(outputRoot)
  }

  private def readOutputFiles(
                               runDirectory: Path
                             ): Map[String, Array[Byte]] = {
    ExpectedFiles.map { fileName =>
      fileName -> Files.readAllBytes(
        runDirectory.resolve(fileName)
      )
    }.toMap
  }

  private def assertCustomerCount(
                                   outputFiles: Map[String, Array[Byte]],
                                   expectedCount: Int
                                 ): Unit = {
    val customerBytes = outputFiles("customers.jsonl")

    val actualCount =
      customerBytes.count(_ == '\n'.toByte)

    assert(
      actualCount == expectedCount,
      s"Expected $expectedCount customer records, found $actualCount"
    )
  }

  test("should generate, validate, serialize, and publish a complete customer graph") {
    withOutputRoot("shopsphere-main-integration") { outputRoot =>

      Main.main(Array(outputRoot.toString))

      val runDirectory =
        publishedRunDirectory(outputRoot)

      assertExpectedFiles(runDirectory)

      val customers =
        readRecords(runDirectory, "customers.jsonl")

      val addresses =
        readRecords(runDirectory, "addresses.jsonl")

      val orders =
        readRecords(runDirectory, "orders.jsonl")

      val orderItems =
        readRecords(runDirectory, "order_items.jsonl")

      val payments =
        readRecords(runDirectory, "payments.jsonl")

      assert(customers.size == 100)

      assert(
        customers.map(_.get("id").asText()).distinct.size == 100
      )

      assert(customers.head.get("id").asText() == "CUST000001")
      assert(customers.last.get("id").asText() == "CUST000100")

      assert(addresses.size == 200)
      assert(orders.size == 200)
      assert(orderItems.size == 400)

      val customerIds =
        customers.map(_.get("id").asText()).toSet

      val orderIds =
        orders.map(_.get("id").asText()).toSet

      assert(
        orderIds.size == orders.size,
        "Order IDs must be unique"
      )

      orderItems.foreach { item =>
        assert(
          orderIds.contains(item.get("order_id").asText()),
          s"Order item references unknown order: ${item.get("order_id").asText()}"
        )
      }

      payments.foreach { payment =>
        assert(
          orderIds.contains(payment.get("order_id").asText()),
          s"Payment references unknown order: ${payment.get("order_id").asText()}"
        )
      }

      addresses.foreach { address =>
        assert(
          customerIds.contains(address.get("customer_id").asText())
        )
      }

      orders.foreach { order =>
        assert(
          customerIds.contains(order.get("customer_id").asText())
        )
      }

      addresses.foreach { address =>
        assert(address.hasNonNull("building_name"))
        assert(address.hasNonNull("society_name"))
        assert(address.hasNonNull("road_name"))
        assert(address.hasNonNull("area_name"))
        assert(address.hasNonNull("city_name"))
        assert(address.hasNonNull("state_name"))
        assert(address.hasNonNull("country_name"))
      }

      assert(payments.nonEmpty)
    }
  }

  test("should publish all five empty files when total customers is zero") {
    withOutputRoot("shopsphere-zero-customers") { outputRoot =>

      val runConfig = GenerationRunConfig(
        totalCustomers = 0,
        chunkSize = 25,
        parallelism = 2,
        maxInFlight = 4
      )

      val runDirectory =
        runWithConfig(outputRoot, runConfig)

      assertExpectedFiles(runDirectory)

      ExpectedFiles.foreach { fileName =>
        val file = runDirectory.resolve(fileName)

        assert(Files.exists(file))
        assert(Files.size(file) == 0)
      }
    }
  }

  test("should generate 53 customers across multiple chunks with one worker") {
    withOutputRoot("shopsphere-single-worker") { outputRoot =>

      val runConfig = GenerationRunConfig(
        totalCustomers = 53,
        chunkSize = 10,
        parallelism = 1,
        maxInFlight = 8
      )

      val runDirectory =
        runWithConfig(outputRoot, runConfig)

      assertExpectedFiles(runDirectory)

      val outputFiles =
        readOutputFiles(runDirectory)

      assertCustomerCount(outputFiles, 53)

      val customers =
        readRecords(runDirectory, "customers.jsonl")

      assert(customers.size == 53)
      assert(customers.head.get("id").asText() == "CUST000001")
      assert(customers.last.get("id").asText() == "CUST000053")

      assert(
        customers.map(_.get("id").asText()).distinct.size == 53
      )
    }
  }

  test("should produce identical output with one and four workers") {

    val singleWorkerOutput =
      withOutputRoot("shopsphere-repro-single") { outputRoot =>

        val runConfig = GenerationRunConfig(
          totalCustomers = 53,
          chunkSize = 10,
          parallelism = 1,
          maxInFlight = 8
        )

        val runDirectory =
          runWithConfig(outputRoot, runConfig)

        assertExpectedFiles(runDirectory)

        readOutputFiles(runDirectory)
      }

    val fourWorkerOutput =
      withOutputRoot("shopsphere-repro-parallel") { outputRoot =>

        val runConfig = GenerationRunConfig(
          totalCustomers = 53,
          chunkSize = 10,
          parallelism = 4,
          maxInFlight = 8
        )

        val runDirectory =
          runWithConfig(outputRoot, runConfig)

        assertExpectedFiles(runDirectory)

        readOutputFiles(runDirectory)
      }

    assert(singleWorkerOutput.keySet == ExpectedFiles)
    assert(fourWorkerOutput.keySet == ExpectedFiles)

    ExpectedFiles.foreach { fileName =>
      assert(
        java.util.Arrays.equals(
          singleWorkerOutput(fileName),
          fourWorkerOutput(fileName)
        ),
        s"Output mismatch between one-worker and four-worker runs: $fileName"
      )
    }

    assertCustomerCount(singleWorkerOutput, 53)
    assertCustomerCount(fourWorkerOutput, 53)
  }
}
