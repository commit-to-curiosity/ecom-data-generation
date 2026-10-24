package com.shopsphere.datagen.common.output

import com.fasterxml.jackson.databind.ObjectMapper

import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * Writes successive batches to JSONL files without retaining the
 * complete output dataset in memory.
 *
 * Intended for single-threaded consumption.
 */
final class JsonLinesStreamWriter(
                                   mapper: ObjectMapper,
                                   outputDirectory: Path
                                 ) extends AutoCloseable {

  require(mapper != null, "ObjectMapper must not be null")
  require(outputDirectory != null, "Output directory must not be null")

  Files.createDirectories(outputDirectory)

  private val writers = mutable.Map.empty[String, BufferedWriter]
  private val counts = mutable.Map.empty[String, Long]
  private var closed = false

  def write[T](fileName: String, records: Iterable[T]): Long = {
    require(fileName != null && fileName.nonEmpty, "File name must not be empty")
    require(
      !fileName.contains("/") && !fileName.contains("\\"),
      "File name must not contain path separators"
    )
    require(records != null, "Records must not be null")
    require(!closed, "Writer is already closed")

    val writer = writers.get(fileName) match {
      case Some(existing) => existing
      case None =>
        val created = Files.newBufferedWriter(
          outputDirectory.resolve(fileName),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE
        )
        writers.put(fileName, created)
        created
    }

    var written = 0L

    records.iterator.foreach { record =>
      writer.write(mapper.writeValueAsString(record))
      writer.newLine()
      written = Math.addExact(written, 1L)
    }

    val total = Math.addExact(counts.getOrElse(fileName, 0L), written)
    counts.update(fileName, total)
    total
  }

  def recordCounts: Map[String, Long] = counts.toMap

  override def close(): Unit = {
    if (!closed) {
      closed = true
      var firstFailure: Throwable = null

      writers.values.foreach { writer =>
        try {
          writer.close()
        } catch {
          case NonFatal(error) =>
            if (firstFailure == null) firstFailure = error
            else firstFailure.addSuppressed(error)
        }
      }

      writers.clear()

      if (firstFailure != null) throw firstFailure
    }
  }
}
