package com.shopsphere.datagen.geography.reference

import com.shopsphere.datagen.geography.model._

case class BuildingLocation(
                             country: Country,
                             state: State,
                             city: City,
                             area: Area,
                             road: Road,
                             society: Society,
                             building: Building
                           )