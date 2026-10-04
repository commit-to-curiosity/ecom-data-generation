package com.shopsphere.datagen.common.distribution

trait Distribution[T] {
  def sample(): T
}