package com.shopsphere.datagen.common.enums

sealed trait PreferredDevice {
  def name: String
}

object PreferredDevice {

  case object MOBILE extends PreferredDevice {
    override val name: String = "mobile"
  }

  case object DESKTOP extends PreferredDevice {
    override val name: String = "desktop"
  }

  case object TABLET extends PreferredDevice {
    override val name: String = "tablet"
  }

  val allDevices: Seq[PreferredDevice] = Seq(MOBILE, DESKTOP, TABLET)

  def fromString(deviceString: String): Either[String, PreferredDevice] = {
    allDevices.find(_.name == deviceString) match {
      case Some(device) => Right(device)
      case None => Left(s"Unsupported preferred device: $deviceString")
    }
  }
}