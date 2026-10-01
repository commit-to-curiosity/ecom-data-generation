package com.shopsphere.datagen.common.config

import scala.collection.mutable
import scala.reflect.runtime.universe.TypeTag

class LoaderRegistry {

  private val loaders = mutable.Map[String, ConfigLoader[_]]()

  def register[T: TypeTag](loader: ConfigLoader[T]): Unit = {
    val key = typeKey[T]

    if (loaders.contains(key)) {
      throw new IllegalArgumentException(
        s"Config loader already registered for $key"
      )
    }

    loaders.put(key, loader)
  }

  def get[T: TypeTag]: ConfigLoader[T] = {
    val key = typeKey[T]

    loaders
      .get(key)
      .map(_.asInstanceOf[ConfigLoader[T]])
      .getOrElse {
        throw new IllegalArgumentException(
          s"No config loader registered for ${key}"
        )
      }
  }

  private def typeKey[T: TypeTag]: String =
    implicitly[TypeTag[T]].tpe.toString

}