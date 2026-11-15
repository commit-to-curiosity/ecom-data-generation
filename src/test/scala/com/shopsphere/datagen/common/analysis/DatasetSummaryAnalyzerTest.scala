package com.shopsphere.datagen.common.analysis

import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

class DatasetSummaryAnalyzerTest extends AnyFunSuite {

  private val fileNames = Seq(
    "customers.jsonl",
    "addresses.jsonl",
    "orders.jsonl",
    "order_items.jsonl",
    "payments.jsonl"
  )

  private def withDirectory(test: Path => Unit): Unit = {
    val directory = Files.createTempDirectory("dataset-summary-test")

    try {
      test(directory)
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

  private def writeFile(
    directory: Path,
    fileName: String,
    content: String
  ): Unit = {
    Files.write(
      directory.resolve(fileName),
      content.getBytes(StandardCharsets.UTF_8)
    )
  }

  private def createEmptyDataset(directory: Path): Unit =
    fileNames.foreach(name => writeFile(directory, name, ""))

  test("should count records across all five dataset files") {
    withDirectory { directory =>
      writeFile(directory, "customers.jsonl",
        """{"id":"CUST000001"}""" + "\n" +
        """{"id":"CUST000002"}""" + "\n")

      writeFile(directory, "addresses.jsonl",
        """{"id":"ADDR000001"}""" + "\n")

      writeFile(directory, "orders.jsonl",
        """{"id":"ORD000001"}""" + "\n")

      writeFile(directory, "order_items.jsonl",
        """{"id":"ITEM000001"}""" + "\n" +
        """{"id":"ITEM000002"}""" + "\n")

      writeFile(directory, "payments.jsonl",
        """{"id":"PAY000001"}""" + "\n")

      val result = new DatasetSummaryAnalyzer().analyze(directory)

      assert(result.recordCounts("customers.jsonl") == 2L)
      assert(result.recordCounts("addresses.jsonl") == 1L)
      assert(result.recordCounts("orders.jsonl") == 1L)
      assert(result.recordCounts("order_items.jsonl") == 2L)
      assert(result.recordCounts("payments.jsonl") == 1L)
      assert(result.totalRecords == 7L)
    }
  }

  test("should accept five empty dataset files") {
    withDirectory { directory =>
      createEmptyDataset(directory)

      val result = new DatasetSummaryAnalyzer().analyze(directory)

      assert(result.recordCounts.values.forall(_ == 0L))
      assert(result.totalRecords == 0L)
    }
  }

  test("should reject a missing required dataset file") {
    withDirectory { directory =>
      fileNames.dropRight(1).foreach { name =>
        writeFile(directory, name, "")
      }

      val error = intercept[IllegalArgumentException] {
        new DatasetSummaryAnalyzer().analyze(directory)
      }

      assert(error.getMessage.contains("payments.jsonl"))
    }
  }

  test("should reject malformed JSON and report its line number") {
    withDirectory { directory =>
      createEmptyDataset(directory)

      writeFile(
        directory,
        "customers.jsonl",
        """{"id":"CUST000001"}""" + "\n" +
        """{"id":invalid}""" + "\n"
      )

      val error = intercept[IllegalArgumentException] {
        new DatasetSummaryAnalyzer().analyze(directory)
      }

      assert(error.getMessage.contains("customers.jsonl"))
      assert(error.getMessage.contains("line 2"))
    }
  }

  test("should reject JSON values that are not objects") {
    withDirectory { directory =>
      createEmptyDataset(directory)

      writeFile(directory, "customers.jsonl", """["CUST000001"]""" + "\n")

      val error = intercept[IllegalArgumentException] {
        new DatasetSummaryAnalyzer().analyze(directory)
      }

      assert(error.getMessage.contains("customers.jsonl"))
      assert(error.getMessage.contains("line 1"))
    }
  }

  test("should reject a non-existent run directory") {
    val directory =
      Files.createTempDirectory("dataset-summary-missing")

    val missingDirectory = directory.resolve("missing")

    try {
      intercept[IllegalArgumentException] {
        new DatasetSummaryAnalyzer().analyze(missingDirectory)
      }
    } finally {
      Files.deleteIfExists(directory)
    }
  }

  test("should reject multiple JSON values on one line") {
    withDirectory { directory =>
      createEmptyDataset(directory)

      writeFile(
        directory,
        "customers.jsonl",
        """{"id":"CUST000001"} {"id":"CUST000002"}""" + "\n"
      )

      val error = intercept[IllegalArgumentException] {
        new DatasetSummaryAnalyzer().analyze(directory)
      }

      assert(error.getMessage.contains("customers.jsonl"))
      assert(error.getMessage.contains("line 1"))
    }
  }

  test("should reject total record count overflow") {
    val summary = DatasetSummary(
      recordCounts = Map(
        "customers.jsonl" -> Long.MaxValue,
        "orders.jsonl" -> 1L
      )
    )

    intercept[ArithmeticException] {
      summary.totalRecords
    }
  }
}
