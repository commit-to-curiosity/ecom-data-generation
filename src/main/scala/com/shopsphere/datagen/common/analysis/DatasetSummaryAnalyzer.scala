
package com.shopsphere.datagen.common.analysis

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper}
import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.LocalDate
import scala.util.Using

final class DatasetSummaryAnalyzer(
                                    mapper: ObjectMapper = JsonMapper.mapper
                                  ) {

  private val expectedFiles = Vector(
    "customers.jsonl",
    "addresses.jsonl",
    "orders.jsonl",
    "order_items.jsonl",
    "payments.jsonl"
  )

  def analyze(runDirectory: Path): DatasetSummary =
    analyzeInternal(runDirectory, None)

  def analyze(
               runDirectory: Path,
               asOfDate: LocalDate,
               ageBands: Seq[CustomerAgeBandConfig]
             ): DatasetSummary = {

    require(asOfDate != null, "As-of date must not be null")
    require(ageBands != null && ageBands.nonEmpty,
      "Age bands must not be empty")

    analyzeInternal(
      runDirectory,
      Some((asOfDate, ageBands))
    )
  }

  private def analyzeInternal(
                               runDirectory: Path,
                               profileConfig: Option[(LocalDate, Seq[CustomerAgeBandConfig])]
                             ): DatasetSummary = {

    require(runDirectory != null, "Run directory must not be null")
    require(Files.isDirectory(runDirectory),
      s"Run directory does not exist: $runDirectory")

    val paths = expectedFiles.map { fileName =>
      val path = runDirectory.resolve(fileName)

      require(Files.isRegularFile(path),
        s"Required dataset file is missing: $path")

      fileName -> path
    }

    val customerAccumulator =
      profileConfig.map { case (asOfDate, ageBands) =>
        new CustomerProfileAccumulator(asOfDate, ageBands)
      }

    val counts = paths.map { case (fileName, path) =>
      val count =
        if (fileName == "customers.jsonl") {
          readRecords(path, customerAccumulator)
        } else {
          readRecords(path, None)
        }

      fileName -> count
    }.toMap

    DatasetSummary(
      recordCounts = counts,
      customerProfile = customerAccumulator.map(_.result())
    )
  }

  private def readRecords(
                           path: Path,
                           accumulator: Option[CustomerProfileAccumulator]
                         ): Long =
    Using.resource(
      Files.newBufferedReader(path, StandardCharsets.UTF_8)
    ) { reader =>

      var count = 0L
      var lineNumber = 0L
      var line = reader.readLine()

      while (line != null) {
        lineNumber = Math.addExact(lineNumber, 1L)

        val node = parseJsonObject(path, lineNumber, line)

        accumulator.foreach(
          _.add(node, path, lineNumber)
        )

        count = Math.addExact(count, 1L)
        line = reader.readLine()
      }

      count
    }

  private def parseJsonObject(
                               path: Path,
                               lineNumber: Long,
                               line: String
                             ): JsonNode = {

    val node =
      try {
        Using.resource(mapper.getFactory.createParser(line)) { parser =>
          mapper.reader
            .`with`(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree[JsonNode](parser)
        }
      } catch {
        case error: JsonProcessingException =>
          throw new IllegalArgumentException(
            s"Invalid JSON in $path at line $lineNumber",
            error
          )
      }

    require(
      node != null && node.isObject,
      s"Expected a JSON object in $path at line $lineNumber"
    )

    node
  }
}
