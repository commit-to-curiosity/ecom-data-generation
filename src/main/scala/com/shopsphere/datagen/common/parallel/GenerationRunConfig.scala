package com.shopsphere.datagen.common.parallel

final case class GenerationRunConfig(
                                      masterSeed: Long = 42L,
                                      totalCustomers: Long = 100L,
                                      chunkSize: Long = 25L,
                                      parallelism: Int = 2,
                                      maxInFlight: Int = 4
                                    ) {
  require(masterSeed >= 0L, "Master seed must be non-negative")
  require(totalCustomers >= 0L, "Total customers must be non-negative")
  require(chunkSize > 0L, "Chunk size must be positive")
  require(parallelism > 0, "Parallelism must be positive")
  require(maxInFlight >= parallelism, "Max in-flight chunks must be >= parallelism")
}
