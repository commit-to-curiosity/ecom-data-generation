package com.shopsphere.datagen.common.enums

sealed trait PreferredPaymentMethod {
  def name: String
}

object PreferredPaymentMethod {

  case object UPI extends PreferredPaymentMethod {
    override val name: String = "upi"
  }

  case object CREDIT_CARD extends PreferredPaymentMethod {
    override val name: String = "credit_card"
  }

  case object DEBIT_CARD extends PreferredPaymentMethod {
    override val name: String = "debit_card"
  }

  case object NET_BANKING extends PreferredPaymentMethod {
    override val name: String = "net_banking"
  }

  case object WALLET extends PreferredPaymentMethod {
    override val name: String = "wallet"
  }

  val allPaymentMethods: Seq[PreferredPaymentMethod] =
    Seq(UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET)

  def fromString(
                  paymentMethodString: String
                ): Either[String, PreferredPaymentMethod] = {
    allPaymentMethods.find(_.name == paymentMethodString) match {
      case Some(paymentMethod) => Right(paymentMethod)
      case None =>
        Left(s"Unsupported preferred payment method: $paymentMethodString")
    }
  }
}