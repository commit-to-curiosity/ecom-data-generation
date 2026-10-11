package com.shopsphere.datagen.common.config

import com.shopsphere.datagen.common.enums.ConfigFileFormat
import org.scalatest.funsuite.AnyFunSuite

class ConfigReaderTest extends AnyFunSuite {

  test("should load customer configuration from HOCON") {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.HOCON)
        .readConfigFile

    assert(config.hasPath(DataGenerationConstants.CUSTOMER))
    assert(config.getString("customer.lifecycle.as_of_date") == "2026-01-01")
  }

  test("should load customer configuration from JSON") {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.JSON)
        .readConfigFile

    assert(config.hasPath(DataGenerationConstants.CUSTOMER))
    assert(config.getDouble("customer.demographics.gender.male") == 0.50)
  }

  test("should load customer configuration from YAML") {
    val config =
      ConfigReaderFactory
        .createConfigReader(ConfigFileFormat.YAML)
        .readConfigFile

    assert(config.hasPath(DataGenerationConstants.CUSTOMER))
    assert(config.getInt("customer.lifecycle.registration_history_days") == 3650)
  }
}
