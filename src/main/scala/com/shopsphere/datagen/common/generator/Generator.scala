package com.shopsphere.datagen.common.generator

trait Generator[T] {
  def generate(): T
}