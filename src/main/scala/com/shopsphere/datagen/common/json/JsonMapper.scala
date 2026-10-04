package com.shopsphere.datagen.common.json

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategies}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonMapper {
  val mapper: ObjectMapper =
    new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .registerModule(DefaultScalaModule)
}