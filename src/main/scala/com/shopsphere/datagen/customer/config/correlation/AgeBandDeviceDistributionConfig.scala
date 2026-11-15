package com.shopsphere.datagen.customer.config.correlation

case class AgeBandDeviceDistributionConfig(
                                            minAge: Int,
                                            maxAge: Int,
                                            mobile: Double,
                                            desktop: Double,
                                            tablet: Double
                                          )