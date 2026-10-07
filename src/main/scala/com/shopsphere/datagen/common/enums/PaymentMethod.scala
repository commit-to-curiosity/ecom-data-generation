package com.shopsphere.datagen.common.enums

sealed trait PaymentMethod {
  def name: String
}

object PaymentMethod {

  case object UPI extends PaymentMethod {
    override val name: String = "upi"
  }

  case object CREDIT_CARD extends PaymentMethod {
    override val name: String = "credit_card"
  }

  case object DEBIT_CARD extends PaymentMethod {
    override val name: String = "debit_card"
  }

  case object NET_BANKING extends PaymentMethod {
    override val name: String = "net_banking"
  }

  case object WALLET extends PaymentMethod {
    override val name: String = "wallet"
  }

  val all: Seq[PaymentMethod] =
    Seq(
      UPI,
      CREDIT_CARD,
      DEBIT_CARD,
      NET_BANKING,
      WALLET
    )
}