package com.shopsphere.datagen.product

import com.shopsphere.datagen.product.loader.ProductLoader
import com.shopsphere.datagen.product.reference.ProductReferenceData
import org.scalatest.funsuite.AnyFunSuite

class ProductReferenceDataTest extends AnyFunSuite {

  test("should find product by ID") {
    val products = new ProductLoader().load()
    val referenceData = new ProductReferenceData(products)

    val product =
      referenceData.findProduct("PROD_001").get

    assert(product.id == "PROD_001")
    assert(product.name == "Wireless Earbuds")
    assert(product.category == "electronics")
    assert(product.brand == "SoundMax")
    assert(product.price == BigDecimal("2499.00"))
  }

  test("should return empty for unknown product ID") {
    val products = new ProductLoader().load()
    val referenceData = new ProductReferenceData(products)

    assert(referenceData.findProduct("PROD_999").isEmpty)
  }
}