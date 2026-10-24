package com.shopsphere.datagen.common.json

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategies, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonMapper {
  val mapper: ObjectMapper =
    new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .registerModule(DefaultScalaModule)
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
