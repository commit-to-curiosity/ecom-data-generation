package com.shopsphere.datagen.customer.config

case class CustomerConfig(
                           demographics: CustomerDemographicsConfig,
                           lifecycle: CustomerLifecycleConfig,
                           status: CustomerStatusConfig,
                           segment: CustomerSegmentConfig,
                           acquisition: CustomerAcquisitionConfig
                         )