
package com.shopsphere.datagen

import com.fasterxml.jackson.databind.JsonNode
import com.shopsphere.datagen.common.json.JsonMapper
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

class MainIntegrationTest extends AnyFunSuite {

  test("should generate, validate, serialize, and publish a complete customer graph") {
    val outputRoot = Files.createTempDirectory("shopsphere-main-integration")

    try {
      Main.main(Array(outputRoot.toString))

      val paths = Files.list(outputRoot)
      val runDirectories =
        try paths.iterator().asScala.filter(Files.isDirectory(_)).toList
        finally paths.close()

      assert(runDirectories.size == 1)

      val runDirectory = runDirectories.head

      val expectedFiles = Set(
        "customers.jsonl",
        "addresses.jsonl",
        "orders.jsonl",
        "order_items.jsonl",
        "payments.jsonl"
      )

      val actualFiles = Files.list(runDirectory)
      val fileNames =
        try actualFiles.iterator().asScala.map(_.getFileName.toString).toSet
        finally actualFiles.close()

      assert(fileNames == expectedFiles)

      def readRecords(fileName: String): List[JsonNode] = {
        val lines = Files.readAllLines(
          runDirectory.resolve(fileName),
          StandardCharsets.UTF_8
        )

        lines.asScala.toList.map { line =>
          val json = JsonMapper.mapper.readTree(line)
          assert(json.isObject, s"Expected a JSON object in $fileName")
          json
        }
      }

      val customers = readRecords("customers.jsonl")
      val addresses = readRecords("addresses.jsonl")
      val orders = readRecords("orders.jsonl")
      val orderItems = readRecords("order_items.jsonl")
      val payments = readRecords("payments.jsonl")

      assert(customers.size == 1)
      assert(addresses.size == 2)
      assert(orders.size == 2)
      assert(orderItems.size == 4)

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
}
