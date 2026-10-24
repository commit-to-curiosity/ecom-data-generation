package com.shopsphere.datagen.common.output

import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

class RunOutputPublisherTest extends AnyFunSuite {

  test("should publish a completed run and return record counts") {
    val root = Files.createTempDirectory("run-publisher-success-test")

    try {
      val publisher = new RunOutputPublisher()

      val result = publisher.publish(root, "test001") { temporaryDirectory =>
        Files.write(
          temporaryDirectory.resolve("customers.jsonl"),
          """{"id":"CUST000001"}""".getBytes(StandardCharsets.UTF_8)
        )

        Map("customers.jsonl" -> 1L)
      }

      assert(result.directory == root.resolve("run_test001"))
      assert(Files.exists(result.directory.resolve("customers.jsonl")))
      assert(result.recordCounts == Map("customers.jsonl" -> 1L))
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

  test("should clean up temporary output when writing fails") {
    val root = Files.createTempDirectory("run-publisher-failure-test")

    try {
      val publisher = new RunOutputPublisher()

      intercept[RuntimeException] {
        publisher.publish(root, "failed001") { temporaryDirectory =>
          Files.write(
            temporaryDirectory.resolve("partial.jsonl"),
            "partial".getBytes(StandardCharsets.UTF_8)
          )

          throw new RuntimeException("Simulated write failure")
        }
      }

      val paths = Files.list(root)
      try {
        assert(!paths.findFirst().isPresent)
      } finally {
        paths.close()
      }
    } finally {
      Files.deleteIfExists(root)
    }
  }

  test("should not overwrite an existing run") {
    val root = Files.createTempDirectory("run-publisher-existing-test")
    val existingRun = Files.createDirectory(root.resolve("run_existing001"))

    try {
      val publisher = new RunOutputPublisher()

      intercept[IllegalArgumentException] {
        publisher.publish(root, "existing001") { _ =>
          fail("Writer must not run when the run directory already exists")
        }
      }

      assert(Files.isDirectory(existingRun))
    } finally {
      Files.deleteIfExists(existingRun)
      Files.deleteIfExists(root)
    }
  }
}
