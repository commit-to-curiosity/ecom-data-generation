package com.shopsphere.datagen.common.parallel

import org.scalatest.funsuite.AnyFunSuite

class GenerationChunkPlannerTest extends AnyFunSuite {

  test("zero customers produce no chunks") {
    assert(GenerationChunkPlanner.plan(0L, 10L).isEmpty)
  }

  test("exact chunk multiples produce contiguous ranges") {
    val chunks = GenerationChunkPlanner.plan(20L, 10L).toList

    assert(
      chunks == List(
        GenerationChunk(0L, 1L, 10L),
        GenerationChunk(1L, 11L, 10L)
      )
    )

    assert(chunks.map(_.lastCustomerOrdinal) == List(10L, 20L))
  }

  test("partial final chunk contains only remaining customers") {
    val chunks = GenerationChunkPlanner.plan(23L, 10L).toList

    assert(
      chunks == List(
        GenerationChunk(0L, 1L, 10L),
        GenerationChunk(1L, 11L, 10L),
        GenerationChunk(2L, 21L, 3L)
      )
    )

    assert(chunks.last.lastCustomerOrdinal == 23L)
    assert(chunks.map(_.customerCount).sum == 23L)
  }

  test("Long-sized totals do not require Int-sized customer counts") {
    val chunk =
      GenerationChunkPlanner.plan(Long.MaxValue, Long.MaxValue).next()

    assert(chunk.firstCustomerOrdinal == 1L)
    assert(chunk.customerCount == Long.MaxValue)
    assert(chunk.lastCustomerOrdinal == Long.MaxValue)
  }

  test("negative total customer count is rejected") {
    intercept[IllegalArgumentException] {
      GenerationChunkPlanner.plan(-1L, 10L)
    }
  }

  test("zero chunk size is rejected") {
    intercept[IllegalArgumentException] {
      GenerationChunkPlanner.plan(10L, 0L)
    }
  }

  test("negative chunk size is rejected") {
    intercept[IllegalArgumentException] {
      GenerationChunkPlanner.plan(10L, -1L)
    }
  }
}