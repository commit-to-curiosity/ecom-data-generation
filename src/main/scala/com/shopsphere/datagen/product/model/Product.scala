package com.shopsphere.datagen.product.model

case class Product(
                    id: String,
                    name: String,
                    category: String,
                    brand: String,
                    price: BigDecimal
                  )