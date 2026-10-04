package com.shopsphere.datagen.geography

import com.shopsphere.datagen.geography.loader.GeographyLoader
import org.scalatest.funsuite.AnyFunSuite

class GeographyReferenceDataTest extends AnyFunSuite {

  test("should resolve building to complete geographic hierarchy") {
    val referenceData = new GeographyLoader().load()

    val location = referenceData
      .resolveBuilding("BUILDING_001")
      .get

    assert(location.country.name == "India")
    assert(location.state.name == "Maharashtra")
    assert(location.city.name == "Mumbai")
    assert(location.area.name == "Andheri East")
    assert(location.road.name == "Mahakali Caves Road")
    assert(location.society.name == "Mahakali Residency")
    assert(location.building.name == "A")
  }
}