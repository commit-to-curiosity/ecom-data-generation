package com.shopsphere.datagen.address

import com.shopsphere.datagen.address.generator.AddressGenerator
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.geography.loader.GeographyLoader
import org.scalatest.funsuite.AnyFunSuite

class AddressGeneratorTest extends AnyFunSuite {

  test("should generate a valid address") {
    val geographyReferenceData = new GeographyLoader().load()
    val randomGenerator = new RandomGenerator(42L)

    val addressGenerator =
      new AddressGenerator(
        customerId = "CUST_000001",
        geographyReferenceData = geographyReferenceData,
        randomGenerator = randomGenerator
      )

    val address = addressGenerator.generate()

    assert(address.id == "ADDR_000001")
    assert(address.customerId == "CUST_000001")
    assert(geographyReferenceData.findBuilding(address.buildingId).nonEmpty)
    assert(address.unitNumber.matches("[A-Z]\\d{3}"))
    assert(address.postalCode.nonEmpty)

    val building = geographyReferenceData.findBuilding(address.buildingId).get

    val location =
      geographyReferenceData.resolveBuilding(building.id).get

    val postalCode =
      geographyReferenceData.findPostalCode(address.postalCode).get

    assert(postalCode.areaId == location.area.id)
  }
}