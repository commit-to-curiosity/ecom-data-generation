package com.shopsphere.datagen.customer.generation

import com.shopsphere.datagen.common.distribution.WeightedDistribution
import com.shopsphere.datagen.common.generation.Generator
import com.shopsphere.datagen.common.random.RandomGenerator
import com.shopsphere.datagen.customer.config.CustomerSegmentConfig
import com.shopsphere.datagen.customer.model.CustomerSegment

class CustomerSegmentGenerator(
                                segmentConfig: CustomerSegmentConfig,
                                random: RandomGenerator
                              ) extends Generator[CustomerSegment] {

  private val segmentDistribution =
    new WeightedDistribution[CustomerSegment](
      Seq(
        CustomerSegment.Standard -> segmentConfig.standard,
        CustomerSegment.Premium -> segmentConfig.premium,
        CustomerSegment.Vip -> segmentConfig.vip,
        CustomerSegment.Business -> segmentConfig.business
      ),
      random
    )

  override def generate(): CustomerSegment = {
    segmentDistribution.sample()
  }
}