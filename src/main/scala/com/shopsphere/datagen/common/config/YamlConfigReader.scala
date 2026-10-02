package com.shopsphere.datagen.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.typesafe.config.{Config, ConfigFactory}

class YamlConfigReader extends ConfigReader {

  override def readConfigFile: Config = {

    val mapper = new ObjectMapper(new YAMLFactory())
    val inputStream = getClass.getClassLoader.getResourceAsStream("customer.yaml")

    val rootNode = mapper.readTree(inputStream)

    val map =
      mapper.convertValue(
        rootNode,
        classOf[java.util.Map[String, Object]]
      )

    ConfigFactory.parseMap(map)
  }
}