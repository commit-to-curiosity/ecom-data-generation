package com.shopsphere.datagen.address.model

case class AddressExport(
                          id: String,
                          customerId: String,
                          buildingId: String,
                          unitNumber: String,
                          postalCode: String,
                          buildingName: String,
                          societyName: String,
                          roadName: String,
                          areaName: String,
                          cityName: String,
                          stateName: String,
                          countryName: String
                        )
