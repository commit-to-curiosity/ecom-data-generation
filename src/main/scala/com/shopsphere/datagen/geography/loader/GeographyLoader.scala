package com.shopsphere.datagen.geography.loader

import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.geography.reference.GeographyReferenceData

import java.io.InputStream

class GeographyLoader {

  private val mapper = JsonMapper.mapper

  def load(): GeographyReferenceData = {
    val countries = read("geography/countries.json", classOf[CountriesData]).countries
    val states = read("geography/states.json", classOf[StatesData]).states
    val cities = read("geography/cities.json", classOf[CitiesData]).cities
    val areas = read("geography/areas.json", classOf[AreasData]).areas
    val roads = read("geography/roads.json", classOf[RoadsData]).roads
    val societies = read("geography/societies.json", classOf[SocietiesData]).societies
    val buildings = read("geography/buildings.json", classOf[BuildingsData]).buildings
    val postalCodes = read("geography/postal_codes.json", classOf[PostalCodesData]).postalCodes

    new GeographyReferenceData(
      countries = countries,
      states = states,
      cities = cities,
      areas = areas,
      roads = roads,
      societies = societies,
      buildings = buildings,
      postalCodes = postalCodes
    )
  }

  private def read[T](resourcePath: String, clazz: Class[T]): T = {
    val stream: InputStream = getClass.getClassLoader.getResourceAsStream(resourcePath)

    if (stream == null) {
      throw new IllegalArgumentException(
        s"Geography resource not found: $resourcePath"
      )
    }

    try {
      mapper.readValue(stream, clazz)
    } finally {
      stream.close()
    }
  }
}