
package com.shopsphere.datagen.common.output

import com.fasterxml.jackson.databind.JsonNode
import com.shopsphere.datagen.address.model.AddressExport
import com.shopsphere.datagen.common.json.JsonMapper
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

class JsonLinesExportIntegrationTest extends AnyFunSuite {

  private val mapper = JsonMapper.mapper

  test("should publish all five JSONL entity files with valid records and matching counts") {
    val root = Files.createTempDirectory("jsonl-export-integration-test")

    try {
      val writer = new JsonLinesWriter(mapper)
      val publisher = new RunOutputPublisher()

      val address = AddressExport(
        id = "ADDR000001_CUST000001",
        customerId = "CUST000001",
        buildingId = "BUILDING_005",
        unitNumber = "B202",
        postalCode = "400076",
        buildingName = "B",
        societyName = "Hiranandani Gardens",
        roadName = "Hiranandani Gardens Road",
        areaName = "Powai",
        cityName = "Mumbai",
        stateName = "Maharashtra",
        countryName = "India"
      )

      val result = publisher.publish(root, "integration001") { directory =>
        Map(
          "customers.jsonl" ->
            writer.write(directory.resolve("customers.jsonl"), List(Map("id" -> "CUST000001"))),
          "addresses.jsonl" ->
            writer.write(directory.resolve("addresses.jsonl"), List(address)),
          "orders.jsonl" ->
            writer.write(directory.resolve("orders.jsonl"), List(Map("id" -> "ORD000001_CUST000001"))),
          "order_items.jsonl" ->
            writer.write(directory.resolve("order_items.jsonl"), List(Map("id" -> "ITEM000001_ORD000001_CUST000001"))),
          "payments.jsonl" ->
            writer.write(directory.resolve("payments.jsonl"), List(Map("id" -> "PAY000001_ORD000001_CUST000001")))
        )
      }

      val expectedFiles = Set(
        "customers.jsonl",
        "addresses.jsonl",
        "orders.jsonl",
        "order_items.jsonl",
        "payments.jsonl"
      )

      assert(result.recordCounts.keySet == expectedFiles)

      expectedFiles.foreach { fileName =>
        val file = result.directory.resolve(fileName)
        assert(Files.isRegularFile(file), s"Missing output file: $fileName")

        val lines = Files.readAllLines(file, StandardCharsets.UTF_8)
        assert(lines.size() == result.recordCounts(fileName).toInt)

        import scala.jdk.CollectionConverters._
        lines.asScala.foreach { line =>
          assert(mapper.readTree(line).isObject, s"Expected a JSON object in $fileName")
        }
      }

      val addressLines = Files.readAllLines(
        result.directory.resolve("addresses.jsonl"),
        StandardCharsets.UTF_8
      )
      val addressJson: JsonNode = mapper.readTree(addressLines.get(0))

      assert(addressJson.get("society_name").asText() == "Hiranandani Gardens")
      assert(addressJson.get("road_name").asText() == "Hiranandani Gardens Road")
      assert(addressJson.get("area_name").asText() == "Powai")
      assert(addressJson.get("city_name").asText() == "Mumbai")
      assert(addressJson.get("state_name").asText() == "Maharashtra")
      assert(addressJson.get("country_name").asText() == "India")
    } finally {
      val paths = Files.walk(root)
      try {
        import scala.jdk.CollectionConverters._
        paths.iterator().asScala.toSeq
          .sortBy(_.getNameCount)
          .reverse
          .foreach(Files.deleteIfExists)
      } finally {
        paths.close()
      }
    }
  }
}
