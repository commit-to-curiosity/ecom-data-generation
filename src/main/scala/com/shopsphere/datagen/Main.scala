package com.shopsphere.datagen

import com.shopsphere.datagen.common.config.{ConfigReaderFactory, DataGenerationConstants}
import com.shopsphere.datagen.common.distribution.RandomGenerator
import com.shopsphere.datagen.common.enums.ConfigFileFormat
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.generator.{CustomerGenerator, CustomerIdentityGenerator}
import com.shopsphere.datagen.customer.loader.CustomerLoaderFactory
import org.apache.logging.log4j.LogManager

object Main {

  private val logger = LogManager.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    logger.info("ShopSphere E-Commerce Data Generator started")

    val configFormat = ConfigFileFormat.HOCON
    logger.info(s"Configuration format: ${configFormat.name}")

    val configReader = ConfigReaderFactory.createConfigReader(configFormat)
    val config = configReader.readConfigFile

    val customerConfigLoader = CustomerLoaderFactory.createCustomerConfigLoader()
    val customerConfig: CustomerConfig =
      customerConfigLoader.loadConfiguration(
        config.getConfig(DataGenerationConstants.CUSTOMER)
      )

    logger.info("Customer configuration loaded successfully")

    val randomSeed = 42L
    logger.info(s"Random seed: $randomSeed")

    val randomGenerator = new RandomGenerator(randomSeed)

    val identityGenerator = new CustomerIdentityGenerator(randomGenerator)

    val customerGenerator = new CustomerGenerator(
      customerConfig,
      randomGenerator,
      identityGenerator
    )

    logger.info("Customer generator initialized")
    val customer = customerGenerator.generate()

  }
}