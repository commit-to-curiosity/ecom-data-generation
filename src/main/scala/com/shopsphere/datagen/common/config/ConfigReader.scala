package com.shopsphere.datagen.common.config

import com.typesafe.config.Config

trait ConfigReader {
  def readConfigFile: Config
}