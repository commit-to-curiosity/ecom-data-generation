package com.shopsphere.datagen.common.config

import com.typesafe.config.{Config, ConfigFactory}

class JsonConfigReader extends ConfigReader {

  override def readConfigFile: Config = {
    ConfigFactory.parseResources("customer.json")
  }

}