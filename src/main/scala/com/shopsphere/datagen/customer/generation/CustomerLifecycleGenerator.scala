package com.shopsphere.datagen.customer.generation

import com.shopsphere.datagen.common.generation.Generator
import com.shopsphere.datagen.common.random.RandomGenerator
import com.shopsphere.datagen.customer.config.CustomerLifecycleConfig

import java.time.LocalDate

class CustomerLifecycleGenerator(
                                  lifecycleConfig: CustomerLifecycleConfig,
                                  random: RandomGenerator
                                ) extends Generator[LocalDate] {

  override def generate(): LocalDate = {
    val daysAgo =
      random.nextInt(
        0,
        lifecycleConfig.registrationHistoryDays
      )

    lifecycleConfig.asOfDate.minusDays(daysAgo.toLong)
  }
}