package com.shopsphere.datagen.common.output

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

final case class PublishedRun(
                               directory: Path,
                               recordCounts: Map[String, Long]
                             )

final class RunOutputPublisher {

  def publish(
               outputRoot: Path,
               runId: String
             )(
               writeOutput: Path => Map[String, Long]
             ): PublishedRun = {
    require(
      runId.matches("[A-Za-z0-9][A-Za-z0-9_-]*"),
      "runId must contain only letters, digits, underscores, or hyphens"
    )

    Files.createDirectories(outputRoot)

    val finalDirectory = outputRoot.resolve(s"run_$runId")

    require(
      !Files.exists(finalDirectory),
      s"Output run already exists: $finalDirectory"
    )

    val temporaryDirectory =
      Files.createTempDirectory(outputRoot, ".run-" + runId + "-")

    try {
      val recordCounts = writeOutput(temporaryDirectory)

      // Do not replace an existing run directory.
      Files.move(temporaryDirectory, finalDirectory)

      PublishedRun(finalDirectory, recordCounts)
    } catch {
      case NonFatal(error) =>
        try {
          deleteRecursively(temporaryDirectory)
        } catch {
          case NonFatal(cleanupError) =>
            error.addSuppressed(cleanupError)
        }

        throw error
    }
  }

  private def deleteRecursively(path: Path): Unit = {
    if (Files.exists(path)) {
      val paths = Files.walk(path)

      try {
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
