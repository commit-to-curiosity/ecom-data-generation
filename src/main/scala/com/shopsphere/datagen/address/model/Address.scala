package com.shopsphere.datagen.address.model

case class Address(
                    id: String,
                    customerId: String,
                    buildingId: String,
                    unitNumber: String,
                    postalCode: String
                  )