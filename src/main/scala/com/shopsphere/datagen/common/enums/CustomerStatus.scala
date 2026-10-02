package com.shopsphere.datagen.common.enums

sealed trait CustomerStatus {
  def name: String
}

object CustomerStatus {

  case object ACTIVE extends CustomerStatus {
    override def name: String = "active"
  }
  case object INACTIVE extends CustomerStatus {
    override def name: String = "inactive"
  }
  case object SUSPENDED extends CustomerStatus {
    override def name: String = "suspended"
  }
  case object CLOSED extends CustomerStatus {
    override def name: String = "closed"
  }

  val allStatuses: Seq[CustomerStatus] = Seq(ACTIVE, INACTIVE, SUSPENDED, CLOSED)

  def fromString(statusStr: String): Either[String, CustomerStatus] = {
    allStatuses.find(_.name == statusStr) match {
      case Some(status) => Right(status)
      case None => Left(s"Unsupported customer status: $statusStr")
    }
  }
}
