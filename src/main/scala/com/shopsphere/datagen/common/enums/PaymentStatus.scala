package com.shopsphere.datagen.common.enums

sealed trait PaymentStatus {
  def name: String
}

object PaymentStatus {

  case object SUCCESSFUL extends PaymentStatus {
    override val name: String = "successful"
  }

  case object FAILED extends PaymentStatus {
    override val name: String = "failed"
  }

  case object PENDING extends PaymentStatus {
    override val name: String = "pending"
  }

  val all: Seq[PaymentStatus] =
    Seq(
      SUCCESSFUL,
      FAILED,
      PENDING
    )
}