package com.shopsphere.datagen.common.parallel

/**
 * Plans contiguous, deterministic customer-ordinal ranges.
 *
 * The iterator avoids allocating all chunk descriptors at once.
 */
object GenerationChunkPlanner {

  def plan(
      totalCustomers: Long,
      chunkSize: Long
  ): Iterator[GenerationChunk] = {
    require(totalCustomers >= 0L, "Total customers must be non-negative")
    require(chunkSize > 0L, "Chunk size must be positive")

    new Iterator[GenerationChunk] {
      private var nextChunkIndex = 0L
      private var nextFirstOrdinal = 1L
      private var remainingCustomers = totalCustomers

      override def hasNext: Boolean =
        remainingCustomers > 0L

      override def next(): GenerationChunk = {
        if (!hasNext) {
          throw new NoSuchElementException("No generation chunks remain")
        }

        val count = math.min(remainingCustomers, chunkSize)

        val chunk = GenerationChunk(
          index = nextChunkIndex,
          firstCustomerOrdinal = nextFirstOrdinal,
          customerCount = count
        )

        remainingCustomers -= count

        if (remainingCustomers > 0L) {
          nextChunkIndex = Math.addExact(nextChunkIndex, 1L)
          nextFirstOrdinal = Math.addExact(nextFirstOrdinal, count)
        }

        chunk
      }
    }
  }
}