package com.shopsphere.datagen.common.parallel

import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.atomic.AtomicInteger

class ParallelChunkExecutorTest extends AnyFunSuite {

  test("should consume results in input order when tasks finish out of order") {
    val executor = new ParallelChunkExecutor(
      parallelism = 3,
      maxInFlight = 3
    )

    val consumed = scala.collection.mutable.ArrayBuffer.empty[(Int, Int)]

    executor.execute(Iterator(0, 1, 2, 3)) { value =>
      if (value == 0) Thread.sleep(150)
      value * 10
    } { (input, result) =>
      consumed += ((input, result))
    }

    assert(consumed.toSeq == Seq(
      (0, 0),
      (1, 10),
      (2, 20),
      (3, 30)
    ))
  }

  test("should respect the configured in-flight limit") {
    val executor = new ParallelChunkExecutor(
      parallelism = 2,
      maxInFlight = 2
    )

    val active = new AtomicInteger(0)
    val maximumActive = new AtomicInteger(0)

    executor.execute(Iterator.range(0, 20)) { value =>
      val current = active.incrementAndGet()
      maximumActive.updateAndGet(previous => math.max(previous, current))

      try {
        Thread.sleep(10)
        value
      } finally {
        active.decrementAndGet()
      }
    } { (_, _) =>
      ()
    }

    assert(maximumActive.get() <= 2)
  }

  test("should propagate generation failures") {
    val executor = new ParallelChunkExecutor(
      parallelism = 2,
      maxInFlight = 2
    )

    val error = intercept[IllegalStateException] {
      executor.execute(Iterator(1, 2, 3)) { value =>
        if (value == 2) {
          throw new IllegalStateException("generation failed")
        }
        value
      } { (_, _) =>
        ()
      }
    }

    assert(error.getMessage == "generation failed")
  }

  test("should propagate consumer failures") {
    val executor = new ParallelChunkExecutor(
      parallelism = 2,
      maxInFlight = 2
    )

    val error = intercept[IllegalArgumentException] {
      executor.execute(Iterator(1, 2, 3)) { identity[Int] } {
        (input, _) =>
          if (input == 1) {
            throw new IllegalArgumentException("consumer failed")
          }
      }
    }

    assert(error.getMessage == "consumer failed")
  }

  test("should handle empty input") {
    val executor = new ParallelChunkExecutor(
      parallelism = 2,
      maxInFlight = 2
    )

    var consumed = 0

    executor.execute(Iterator.empty[Int]) { identity[Int] } { (_, _) =>
      consumed += 1
    }

    assert(consumed == 0)
  }

  test("should reject invalid concurrency settings") {
    intercept[IllegalArgumentException] {
      new ParallelChunkExecutor(parallelism = 0, maxInFlight = 1)
    }

    intercept[IllegalArgumentException] {
      new ParallelChunkExecutor(parallelism = 3, maxInFlight = 2)
    }
  }

  test("should retain later results while waiting for the first task") {
    val executor = new ParallelChunkExecutor(
      parallelism = 3,
      maxInFlight = 3
    )

    val consumed = scala.collection.mutable.ArrayBuffer.empty[Int]

    executor.execute(Iterator(0, 1, 2)) { value =>
      if (value == 0) Thread.sleep(200)
      value
    } { (input, result) =>
      assert(input == result)
      consumed += result
    }

    assert(consumed.toSeq == Seq(0, 1, 2))
  }

  test("should not consume later results after a generation failure") {
    val executor = new ParallelChunkExecutor(
      parallelism = 2,
      maxInFlight = 2
    )

    val consumed = scala.collection.mutable.ArrayBuffer.empty[Int]

    intercept[IllegalStateException] {
      executor.execute(Iterator(0, 1, 2, 3)) { value =>
        if (value == 1) {
          throw new IllegalStateException("chunk failed")
        }
        value
      } { (input, _) =>
        consumed += input
      }
    }

    assert(!consumed.contains(1))
    assert(!consumed.contains(2))
    assert(!consumed.contains(3))
  }
}