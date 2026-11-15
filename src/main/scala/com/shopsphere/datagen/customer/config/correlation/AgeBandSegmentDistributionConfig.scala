package com.shopsphere.datagen.customer.config.correlation

case class AgeBandSegmentDistributionConfig(
                                             minAge: Int,
                                             maxAge: Int,
                                             standard: Double,
                                             premium: Double,
                                             vip: Double,
                                             business: Double
                                           )