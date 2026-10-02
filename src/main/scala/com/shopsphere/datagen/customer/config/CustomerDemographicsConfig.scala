package com.shopsphere.datagen.customer.config

case class CustomerDemographicsConfig(
                                       ageBands: Seq[CustomerAgeBandConfig],
                                       gender: CustomerGenderConfig
                                     )