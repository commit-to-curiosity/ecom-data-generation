package com.shopsphere.datagen.geography.validation

import com.shopsphere.datagen.geography.reference.GeographyReferenceData

class GeographyValidator(referenceData: GeographyReferenceData) {

  def validate(): Unit = {
    validateDuplicateIds()
    validateStates()
    validateCities()
    validateAreas()
    validateRoads()
    validateSocieties()
    validateBuildings()
    validatePostalCodes()
  }

  private def validateDuplicateIds(): Unit = {
    validateUnique("country IDs", referenceData.countries.map(_.id))
    validateUnique("state IDs", referenceData.states.map(_.id))
    validateUnique("city IDs", referenceData.cities.map(_.id))
    validateUnique("area IDs", referenceData.areas.map(_.id))
    validateUnique("road IDs", referenceData.roads.map(_.id))
    validateUnique("society IDs", referenceData.societies.map(_.id))
    validateUnique("building IDs", referenceData.buildings.map(_.id))
    validateUnique("postal codes", referenceData.postalCodes.map(_.code))
  }

  private def validateUnique(name: String, values: Seq[String]): Unit = {
    val duplicates = values
      .groupBy(identity)
      .collect {
        case (value, occurrences) if occurrences.size > 1 => value
      }

    if (duplicates.nonEmpty) {
      throw new IllegalArgumentException(
        s"Duplicate $name found: ${duplicates.mkString(", ")}"
      )
    }
  }

  private def validateStates(): Unit = {
    referenceData.statesById.values.foreach { state =>
      if (!referenceData.countriesById.contains(state.countryId)) {
        throw new IllegalArgumentException(
          s"State '${state.id}' references unknown country '${state.countryId}'"
        )
      }
    }
  }

  private def validateCities(): Unit = {
    referenceData.citiesById.values.foreach { city =>
      if (!referenceData.statesById.contains(city.stateId)) {
        throw new IllegalArgumentException(
          s"City '${city.id}' references unknown state '${city.stateId}'"
        )
      }
    }
  }

  private def validateAreas(): Unit = {
    referenceData.areasById.values.foreach { area =>
      if (!referenceData.citiesById.contains(area.cityId)) {
        throw new IllegalArgumentException(
          s"Area '${area.id}' references unknown city '${area.cityId}'"
        )
      }
    }
  }

  private def validateRoads(): Unit = {
    referenceData.roadsById.values.foreach { road =>
      if (!referenceData.areasById.contains(road.areaId)) {
        throw new IllegalArgumentException(
          s"Road '${road.id}' references unknown area '${road.areaId}'"
        )
      }
    }
  }

  private def validateSocieties(): Unit = {
    referenceData.societiesById.values.foreach { society =>
      if (!referenceData.roadsById.contains(society.roadId)) {
        throw new IllegalArgumentException(
          s"Society '${society.id}' references unknown road '${society.roadId}'"
        )
      }
    }
  }

  private def validateBuildings(): Unit = {
    referenceData.buildingsById.values.foreach { building =>
      if (!referenceData.societiesById.contains(building.societyId)) {
        throw new IllegalArgumentException(
          s"Building '${building.id}' references unknown society '${building.societyId}'"
        )
      }
    }
  }

  private def validatePostalCodes(): Unit = {
    referenceData.postalCodesByCode.values.foreach { postalCode =>
      if (!referenceData.areasById.contains(postalCode.areaId)) {
        throw new IllegalArgumentException(
          s"Postal code '${postalCode.code}' references unknown area '${postalCode.areaId}'"
        )
      }
    }
  }


}