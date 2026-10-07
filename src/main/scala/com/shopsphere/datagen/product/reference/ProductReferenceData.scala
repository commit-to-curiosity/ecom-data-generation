package com.shopsphere.datagen.product.reference

import com.shopsphere.datagen.product.model.Product

class ProductReferenceData(val products: Seq[Product]) {

  val productsById: Map[String, Product] =
    products.map(product => product.id -> product).toMap

  def findProduct(productId: String): Option[Product] =
    productsById.get(productId)
}