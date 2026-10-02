package com.shopsphere.datagen

import com.shopsphere.datagen.common.config.LoaderRegistry
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.loader.{
  CustomerConfigLoader,
  CustomerLoaderRegistration
}
import com.typesafe.config.ConfigFactory


object Main {

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()

    val registry = new LoaderRegistry()

    CustomerLoaderRegistration.register(registry)

    val customerConfigLoader =
      new CustomerConfigLoader(registry)

    val customerConfig: CustomerConfig =
      customerConfigLoader.load(config)

    println("========== Customer Configuration ==========")
    println(customerConfig)
    println("============================================")
  }
}
