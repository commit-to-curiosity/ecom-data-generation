package com.shopsphere.datagen.geography

import com.shopsphere.datagen.geography.loader.GeographyLoader
import com.shopsphere.datagen.geography.reference.GeographyReferenceData
import com.shopsphere.datagen.geography.validation.GeographyValidator
import org.scalatest.funsuite.AnyFunSuite

class GeographyValidatorTest extends AnyFunSuite {

  test("geography reference data should be valid") {
    val referenceData = new GeographyLoader().load()
    val validator = new GeographyValidator(referenceData)

    validator.validate()
  }

  test("should reject duplicate country IDs") {
    val referenceData = new GeographyLoader().load()

    val duplicateCountry = referenceData.countries.head.copy()

    val invalidReferenceData =
      new GeographyReferenceData(
        countries = referenceData.countries :+ duplicateCountry,
        states = referenceData.states,
        cities = referenceData.cities,
        areas = referenceData.areas,
        roads = referenceData.roads,
        societies = referenceData.societies,
        buildings = referenceData.buildings,
        postalCodes = referenceData.postalCodes
      )

    val validator = new GeographyValidator(invalidReferenceData)

    assertThrows[IllegalArgumentException] {
      validator.validate()
    }
  }
}