package com.shopsphere.datagen.common.parallel

/**
 * A deterministic range of global customer ordinals.
 *
 * Chunk indices are zero-based; customer ordinals are one-based.
 */
final case class GenerationChunk(
    index: Long,
    firstCustomerOrdinal: Long,
    customerCount: Long
) {
  require(index >= 0L, "Chunk index must be non-negative")
  require(firstCustomerOrdinal > 0L, "First customer ordinal must be positive")
  require(customerCount > 0L, "Chunk customer count must be positive")

  def lastCustomerOrdinal: Long =
    Math.addExact(firstCustomerOrdinal, customerCount - 1L)
}