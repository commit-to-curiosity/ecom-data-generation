package com.shopsphere.datagen.common.enums

sealed trait ConfigFileFormat {
  def name: String
}

object ConfigFileFormat {

  case object JSON extends ConfigFileFormat {
    override def name: String = "json"
  }

  case object YAML extends ConfigFileFormat {
    override def name: String = "yaml"
  }

  case object HOCON extends ConfigFileFormat {
    override def name: String = "hocon"
  }

  val allFileFormats: Seq[ConfigFileFormat] = Seq(JSON, YAML, HOCON)

  def fromString(format: String): Either[String, ConfigFileFormat] = {
    allFileFormats.find(_.name == format) match {
      case Some(configFormat) => Right(configFormat)
      case None => Left(s"Unsupported configuration format: $format")
    }
  }
}