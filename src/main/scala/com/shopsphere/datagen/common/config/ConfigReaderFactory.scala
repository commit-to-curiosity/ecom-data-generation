package com.shopsphere.datagen.common.config

import com.shopsphere.datagen.common.enums.ConfigFileFormat

object ConfigReaderFactory {

  def createConfigReader(format: ConfigFileFormat): ConfigReader = {

    format match {

      case ConfigFileFormat.HOCON =>
        new HoconConfigReader()

      case ConfigFileFormat.JSON =>
        new JsonConfigReader()

      case ConfigFileFormat.YAML =>
        new YamlConfigReader()
    }
  }
}