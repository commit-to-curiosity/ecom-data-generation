package com.shopsphere.datagen.address.generator

import com.shopsphere.datagen.address.model.Address
import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.geography.model.Building
import com.shopsphere.datagen.geography.reference.GeographyReferenceData

class AddressGenerator(
                        customerId: String,
                        geographyReferenceData: GeographyReferenceData,
                        randomGenerator: RandomGenerator
                      ) extends Generator[Address] {

  private var nextAddressId = 1L

  private val buildings = geographyReferenceData.buildings

  override def generate(): Address = {
    val building = new UniformDistribution(
      buildings,
      randomGenerator
    ).sample()

    val location = geographyReferenceData
      .resolveBuilding(building.id)
      .get

    val unitNumber = generateUnitNumber(building)

    val postalCode = generatePostalCode(location.area.id)

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
    val floor =
      new UniformDistribution(
        1 to building.floors,
        randomGenerator
      ).sample()

    val unit =
      new UniformDistribution(
        1 to building.unitsPerFloor,
        randomGenerator
      ).sample()

    f"$floor%01d$unit%02d"
  }

  private def generatePostalCode(areaId: String): String = {
    val postalCodes =
      geographyReferenceData.findPostalCodesByArea(areaId)

    new UniformDistribution(
      postalCodes.map(_.code),
      randomGenerator
    ).sample()
  }
}