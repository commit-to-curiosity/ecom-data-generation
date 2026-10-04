package com.shopsphere.datagen.geography.reference

import com.shopsphere.datagen.geography.model._

class GeographyReferenceData(
                              val countries: Seq[Country],
                              val states: Seq[State],
                              val cities: Seq[City],
                              val areas: Seq[Area],
                              val roads: Seq[Road],
                              val societies: Seq[Society],
                              val buildings: Seq[Building],
                              val postalCodes: Seq[PostalCode]
                            ) {

  val countriesById: Map[String, Country] =
    countries.map(country => country.id -> country).toMap

  val statesById: Map[String, State] =
    states.map(state => state.id -> state).toMap

  val citiesById: Map[String, City] =
    cities.map(city => city.id -> city).toMap

  val areasById: Map[String, Area] =
    areas.map(area => area.id -> area).toMap

  val roadsById: Map[String, Road] =
    roads.map(road => road.id -> road).toMap

  val societiesById: Map[String, Society] =
    societies.map(society => society.id -> society).toMap

  val buildingsById: Map[String, Building] =
    buildings.map(building => building.id -> building).toMap

  val postalCodesByCode: Map[String, PostalCode] =
    postalCodes.map(postalCode => postalCode.code -> postalCode).toMap

  def findCountry(countryId: String): Option[Country] =
    countriesById.get(countryId)

  def findState(stateId: String): Option[State] =
    statesById.get(stateId)

  def findCity(cityId: String): Option[City] =
    citiesById.get(cityId)

  def findArea(areaId: String): Option[Area] =
    areasById.get(areaId)

  def findRoad(roadId: String): Option[Road] =
    roadsById.get(roadId)

  def findSociety(societyId: String): Option[Society] =
    societiesById.get(societyId)

  def findBuilding(buildingId: String): Option[Building] =
    buildingsById.get(buildingId)

  def findPostalCode(code: String): Option[PostalCode] =
    postalCodesByCode.get(code)

  def resolveBuilding(buildingId: String): Option[BuildingLocation] = {
    for {
      building <- findBuilding(buildingId)
      society <- findSociety(building.societyId)
      road <- findRoad(society.roadId)
      area <- findArea(road.areaId)
      city <- findCity(area.cityId)
      state <- findState(city.stateId)
      country <- findCountry(state.countryId)
    } yield BuildingLocation(
      country = country,
      state = state,
      city = city,
      area = area,
      road = road,
      society = society,
      building = building
    )
  }
}