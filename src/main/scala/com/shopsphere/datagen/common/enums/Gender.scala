package com.shopsphere.datagen.common.enums

sealed trait Gender {
  def name: String
}

object Gender {

  case object FEMALE extends Gender {
    override def name: String = "female"
  }

  case object MALE extends Gender {
    override def name: String = "male"
  }

  case object OTHER extends Gender {
    override def name: String = "other"
  }

  val allGenders: Seq[Gender] = Seq(FEMALE, MALE, OTHER)

  def fromString(genderStr: String): Either[String, Gender] = {
    allGenders.find(_.name == genderStr) match {
      case Some(gender) => Right(gender)
      case None => Left(s"Unsupported gender: $genderStr")
    }
  }
}