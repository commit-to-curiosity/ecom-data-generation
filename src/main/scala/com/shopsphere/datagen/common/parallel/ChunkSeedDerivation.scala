package com.shopsphere.datagen.common.parallel

/**
 * Derives stable chunk seeds using a SplitMix64-style finalizer.
 *
 * Arithmetic intentionally uses Long overflow, equivalent to arithmetic
 * modulo 2^64. The mixing constants and formula are part of the
 * reproducibility contract and must not change silently.
 */
object ChunkSeedDerivation {

  private val GoldenGamma = 0x9E3779B97F4A7C15L
  private val Mix1 = 0xBF58476D1CE4E5B9L
  private val Mix2 = 0x94D049BB133111EBL

  def derive(masterSeed: Long, chunkIndex: Long): Long = {
    require(
      chunkIndex >= 0L && chunkIndex < Long.MaxValue,
      "Chunk index must be non-negative and less than Long.MaxValue"
    )

    mix64(masterSeed + GoldenGamma * (chunkIndex + 1L))
  }

  private def mix64(input: Long): Long = {
    var value = input
    value = (value ^ (value >>> 30)) * Mix1
    value = (value ^ (value >>> 27)) * Mix2
    value ^ (value >>> 31)
  }
}