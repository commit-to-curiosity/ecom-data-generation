package com.shopsphere.datagen.geography.model

case class Country(
                    id: String,
                    name: String
                  )

case class State(
                  id: String,
                  name: String,
                  countryId: String
                )

case class City(
                 id: String,
                 name: String,
                 stateId: String
               )

case class Area(
                 id: String,
                 name: String,
                 cityId: String
               )

case class Road(
                 id: String,
                 name: String,
                 areaId: String
               )

case class Society(
                    id: String,
                    name: String,
                    roadId: String
                  )

case class Building(
                     id: String,
                     name: String,
                     societyId: String,
                     floors: Int,
                     unitsPerFloor: Int
                   )

case class PostalCode(
                       code: String,
                       areaId: String
                     )