package com.shopsphere.datagen.customer.config

case class CustomerConfig(
                           demographics: CustomerDemographicsConfig,
                           lifecycle: CustomerLifecycleConfig,
                           segment: CustomerSegmentConfig,
                           status: CustomerStatusConfig,
                           acquisition: CustomerAcquisitionConfig,
                           behavior: CustomerBehaviorConfig
                         )