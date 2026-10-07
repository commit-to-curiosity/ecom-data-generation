package com.shopsphere.datagen.product.loader

import com.shopsphere.datagen.common.json.JsonMapper
import com.shopsphere.datagen.product.model.{Product, ProductsData}

import java.io.InputStream

class ProductLoader {

  private val mapper = JsonMapper.mapper

  def load(): Seq[Product] = {
    val resourcePath = "product/products.json"

    val stream: InputStream =
      getClass.getClassLoader.getResourceAsStream(resourcePath)

    if (stream == null) {
      throw new IllegalArgumentException(
        s"Product resource not found: $resourcePath"
      )
    }

    try {
      mapper.readValue(stream, classOf[ProductsData]).products
    } finally {
      stream.close()
    }
  }
}