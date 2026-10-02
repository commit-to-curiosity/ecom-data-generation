package com.shopsphere.datagen.common.config

import com.typesafe.config.{Config, ConfigFactory}

class HoconConfigReader extends ConfigReader {

  override def readConfigFile: Config = {
    ConfigFactory.parseResources("customer.conf")
  }

}