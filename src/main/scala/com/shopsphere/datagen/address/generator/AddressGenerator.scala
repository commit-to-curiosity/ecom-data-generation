package com.shopsphere.datagen.address.generator

import com.shopsphere.datagen.address.model.Address
import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.geography.model.{Building, PostalCode}
import com.shopsphere.datagen.geography.reference.{BuildingLocation, GeographyReferenceData}

class AddressGenerator(
                        customerId: String,
                        geographyReferenceData: GeographyReferenceData,
                        randomGenerator: RandomGenerator
                      ) extends Generator[Address] {

  private var nextAddressId = 1L

  private val buildings = geographyReferenceData.buildings

  override def generate(): Address = {
    val building: Building = new UniformDistribution(
      buildings,
      randomGenerator
    ).sample()

    val location: BuildingLocation = geographyReferenceData
      .resolveBuilding(building.id).get

    val unitNumber: String = generateUnitNumber(building)

    val postalCode: String = generatePostalCode(location.area.id)

    Address(
      id = generateAddressId(),
      customerId = customerId,
      buildingId = building.id,
      unitNumber = s"${building.name}$unitNumber",
      postalCode = postalCode
    )
  }

  private def generateAddressId(): String = {
    val addressId = f"ADDR_$nextAddressId%06d"
    nextAddressId += 1
    addressId
  }

  private def generateUnitNumber(building: Building): String = {
    val floor: Int =
      new UniformDistribution(
        1 to building.floors,
        randomGenerator
      ).sample()

    val unit: Int =
      new UniformDistribution(
        1 to building.unitsPerFloor,
        randomGenerator
      ).sample()

    f"$floor%01d$unit%02d"
  }

  private def generatePostalCode(areaId: String): String = {
    val postalCodes: Seq[PostalCode] =
      geographyReferenceData.findPostalCodesByArea(areaId)

    new UniformDistribution(
      postalCodes.map(_.code),
      randomGenerator
    ).sample()
  }
}