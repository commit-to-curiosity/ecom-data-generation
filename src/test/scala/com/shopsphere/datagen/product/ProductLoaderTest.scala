package com.shopsphere.datagen.product

import com.shopsphere.datagen.product.loader.ProductLoader
import org.scalatest.funsuite.AnyFunSuite

class ProductLoaderTest extends AnyFunSuite {

  test("should load products from JSON") {
    val products = new ProductLoader().load()

    assert(products.size == 10)
    assert(products.head.id == "PROD_001")
    assert(products.head.name == "Wireless Earbuds")
    assert(products.head.price == BigDecimal("2499.00"))
  }
}