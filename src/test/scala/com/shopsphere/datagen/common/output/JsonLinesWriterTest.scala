package com.shopsphere.datagen.common.output

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

class JsonLinesWriterTest extends AnyFunSuite {

  private case class SampleRecord(id: String, customerStatus: String)

  private val mapper: ObjectMapper =
    new ObjectMapper()
      .registerModule(DefaultScalaModule)

  test("should write one JSON object per line and return the record count") {
    val directory = Files.createTempDirectory("jsonl-writer-test")
    val path = directory.resolve("records.jsonl")

    try {
      val writer = new JsonLinesWriter(mapper)
      val count = writer.write(
        path,
        List(
          SampleRecord("CUST000001", "ACTIVE"),
          SampleRecord("CUST000002", "INACTIVE")
        )
      )

      val lines = Files.readAllLines(path, StandardCharsets.UTF_8)

      assert(count == 2)
      assert(lines.size() == 2)
      assert(mapper.readTree(lines.get(0)).get("id").asText() == "CUST000001")
      assert(mapper.readTree(lines.get(1)).get("id").asText() == "CUST000002")
    } finally {
      Files.deleteIfExists(path)
      Files.deleteIfExists(directory)
    }
  }

  test("should create an empty file for an empty collection") {
    val directory = Files.createTempDirectory("jsonl-writer-empty-test")
    val path = directory.resolve("records.jsonl")

    try {
      val writer = new JsonLinesWriter(mapper)
      val count = writer.write(path, List.empty[SampleRecord])

      assert(count == 0)
      assert(Files.size(path) == 0)
    } finally {
      Files.deleteIfExists(path)
      Files.deleteIfExists(directory)
    }
  }

  test("should not overwrite an existing file") {
    val directory = Files.createTempDirectory("jsonl-writer-existing-test")
    val path = directory.resolve("records.jsonl")
    val originalContent = "keep-existing-content"

    try {
      Files.write(path, originalContent.getBytes(StandardCharsets.UTF_8))

      val writer = new JsonLinesWriter(mapper)

      intercept[java.nio.file.FileAlreadyExistsException] {
        writer.write(path, List(SampleRecord("CUST000001", "ACTIVE")))
      }

      val actualContent =
        new String(Files.readAllBytes(path), StandardCharsets.UTF_8)

      assert(actualContent == originalContent)
    } finally {
      Files.deleteIfExists(path)
      Files.deleteIfExists(directory)
    }
  }
}
