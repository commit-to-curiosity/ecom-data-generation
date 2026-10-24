package com.shopsphere.datagen.common.output

import com.shopsphere.datagen.common.json.JsonMapper
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class JsonLinesStreamWriterTest extends AnyFunSuite {

  test("should append batches in order and track cumulative counts") {
    val directory = Files.createTempDirectory("jsonl-stream-test")
    val writer = new JsonLinesStreamWriter(JsonMapper.mapper, directory)

    try {
      assert(
        writer.write("customers.jsonl", List(Map("id" -> "CUST000001"))) == 1L
      )
      assert(
        writer.write("customers.jsonl", List(Map("id" -> "CUST000002"))) == 2L
      )
      assert(
        writer.write("orders.jsonl", List(Map("id" -> "ORD000001"))) == 1L
      )

      assert(
        writer.recordCounts == Map(
          "customers.jsonl" -> 2L,
          "orders.jsonl" -> 1L
        )
      )
    } finally {
      writer.close()
    }

    try {
      val customerLines = Files.readAllLines(
        directory.resolve("customers.jsonl"),
        StandardCharsets.UTF_8
      )

      assert(customerLines.size() == 2)
      assert(JsonMapper.mapper.readTree(customerLines.get(0)).get("id").asText() == "CUST000001")
      assert(JsonMapper.mapper.readTree(customerLines.get(1)).get("id").asText() == "CUST000002")
    } finally {
      val paths = Files.walk(directory)
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

  test("should reject writes after close") {
    val directory = Files.createTempDirectory("jsonl-stream-closed-test")
    val writer = new JsonLinesStreamWriter(JsonMapper.mapper, directory)

    writer.close()

    try {
      intercept[IllegalArgumentException] {
        writer.write("customers.jsonl", List(Map("id" -> "CUST000001")))
      }
    } finally {
      Files.deleteIfExists(directory)
    }
  }

  test("should reject path separators in file names") {
    val directory = Files.createTempDirectory("jsonl-stream-path-test")
    val writer = new JsonLinesStreamWriter(JsonMapper.mapper, directory)

    try {
      intercept[IllegalArgumentException] {
        writer.write("../customers.jsonl", List(Map("id" -> "CUST000001")))
      }
    } finally {
      writer.close()
      Files.deleteIfExists(directory)
    }
  }

  test("should close the writer and preserve failure when serialization fails") {
    val directory = Files.createTempDirectory("jsonl-stream-serialization-test")
    val writer = new JsonLinesStreamWriter(JsonMapper.mapper, directory)

    try {
      intercept[com.fasterxml.jackson.core.JsonProcessingException] {
        writer.write(
          "records.jsonl",
          List(Map("id" -> "CUST000001"), new Object())
        )
      }
    } finally {
      writer.close()
    }

    try {
      assert(Files.isRegularFile(directory.resolve("records.jsonl")))
    } finally {
      val paths = Files.walk(directory)
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
