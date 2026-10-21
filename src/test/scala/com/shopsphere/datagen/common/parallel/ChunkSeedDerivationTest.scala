package com.shopsphere.datagen.common.parallel

import org.scalatest.funsuite.AnyFunSuite

class ChunkSeedDerivationTest extends AnyFunSuite {

  test("same master seed and chunk index produce the same seed") {
    val first = ChunkSeedDerivation.derive(42L, 0L)
    val second = ChunkSeedDerivation.derive(42L, 0L)

    assert(first == second)
  }

  test("seed derivation matches the documented algorithm") {
    assert(
      ChunkSeedDerivation.derive(42L, 0L) ==
        -4767286540954276203L
    )

    assert(
      ChunkSeedDerivation.derive(42L, 1L) ==
        2949826092126892291L
    )
  }

  test("different chunk indices produce different seeds") {
    val seeds = (0L until 100L)
      .map(index => ChunkSeedDerivation.derive(42L, index))

    assert(seeds.distinct.size == seeds.size)
  }

  test("different master seeds produce different seeds for a chunk") {
    assert(
      ChunkSeedDerivation.derive(42L, 0L) !=
        ChunkSeedDerivation.derive(43L, 0L)
    )
  }

  test("negative chunk index is rejected") {
    intercept[IllegalArgumentException] {
      ChunkSeedDerivation.derive(42L, -1L)
    }
  }

  test("Long.MaxValue chunk index is rejected") {
    intercept[IllegalArgumentException] {
      ChunkSeedDerivation.derive(42L, Long.MaxValue)
    }
  }
}