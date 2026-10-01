package com.shopsphere.datagen.customer.model

import java.time.LocalDate

case class Customer(
                     id: Long,
                     age: Int,
                     gender: Gender,
                     registrationDate: LocalDate,
                     status: CustomerStatus,
                     segment: CustomerSegment,
                     acquisition: CustomerAcquisition,
                     behaviorProfile: CustomerBehaviorProfile
                   )