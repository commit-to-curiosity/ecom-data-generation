package com.shopsphere.datagen.common.config

import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.customer.config.CustomerGenerationStrategy
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import com.typesafe.config.ConfigValueFactory
import org.scalatest.funsuite.AnyFunSuite

class ConfigReaderTest extends AnyFunSuite {

  private val formats = Seq(
    ConfigFileFormat.HOCON,
    ConfigFileFormat.JSON,
    ConfigFileFormat.YAML
  )

  private def loadRootConfig(format: ConfigFileFormat) =
    ConfigReaderFactory
      .createConfigReader(format)
      .readConfigFile

  private def loadCustomerConfig(format: ConfigFileFormat) = {
    val rootConfig = loadRootConfig(format)

    CustomerLoaderFactory
      .createCustomerConfigLoader()
      .loadConfiguration(
        rootConfig.getConfig(DataGenerationConstants.CUSTOMER)
      )
  }

  test("should load customer configuration from HOCON") {
    val config = loadRootConfig(ConfigFileFormat.HOCON)

    assert(config.hasPath(DataGenerationConstants.CUSTOMER))
    assert(config.getString("customer.lifecycle.as_of_date") == "2026-01-01")
  }

  test("should load customer configuration from JSON") {
    val config = loadRootConfig(ConfigFileFormat.JSON)

    assert(config.hasPath(DataGenerationConstants.CUSTOMER))
    assert(config.getDouble("customer.demographics.gender.male") == 0.50)
  }

  test("should load customer configuration from YAML") {
    val config = loadRootConfig(ConfigFileFormat.YAML)

    assert(config.hasPath(DataGenerationConstants.CUSTOMER))
    assert(config.getInt("customer.lifecycle.registration_history_days") == 3650)
  }

  test("all configuration formats should produce equivalent customer configurations") {
    val customerConfigs = formats.map(loadCustomerConfig)

    assert(customerConfigs.distinct.size == 1)

    val customerConfig = customerConfigs.head

    assert(
      customerConfig.generation.strategy ==
        CustomerGenerationStrategy.INDEPENDENT
    )

    assert(customerConfig.generation.correlations.nonEmpty)

    val correlations = customerConfig.generation.correlations.get

    assert(correlations.segmentByAgeBand.size == 6)
    assert(correlations.preferredDeviceByAgeBand.size == 6)
    assert(correlations.preferredPaymentMethodBySegment.size == 4)
  }

  test("all configuration formats should support correlated generation") {
    val correlatedConfigs = formats.map { format =>
      val rootConfig = loadRootConfig(format).withValue(
        s"${DataGenerationConstants.CUSTOMER}.${DataGenerationConstants.GENERATION}.${DataGenerationConstants.STRATEGY}",
        ConfigValueFactory.fromAnyRef("correlated")
      )

      CustomerLoaderFactory
        .createCustomerConfigLoader()
        .loadConfiguration(
          rootConfig.getConfig(DataGenerationConstants.CUSTOMER)
        )
    }

    assert(correlatedConfigs.distinct.size == 1)

    correlatedConfigs.foreach { customerConfig =>
      assert(
        customerConfig.generation.strategy ==
          CustomerGenerationStrategy.CORRELATED
      )
      assert(customerConfig.generation.correlations.nonEmpty)
    }
  }
}