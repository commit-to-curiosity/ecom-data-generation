package com.shopsphere.datagen.common.enums

sealed trait CustomerSegment {
  def name: String
}

object CustomerSegment {

  case object STANDARD extends CustomerSegment {
    override def name: String = "standard"
  }

  case object PREMIUM extends CustomerSegment {
    override def name: String = "premium"
  }

  case object VIP extends CustomerSegment {
    override def name: String = "vip"
  }

  case object BUSINESS extends CustomerSegment {
    override def name: String = "business"
  }

  val allSegments: Seq[CustomerSegment] =
    Seq(STANDARD, PREMIUM, VIP, BUSINESS)

  def fromString(segmentString: String): Either[String, CustomerSegment] = {

    allSegments.find(_.name == segmentString) match {
      case Some(segment) => Right(segment)
      case None =>
        Left(s"Unsupported customer segment: $segmentString")
    }
  }
}