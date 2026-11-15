package com.shopsphere.datagen.customer.config

sealed trait CustomerGenerationStrategy {
  def name: String
}

object CustomerGenerationStrategy {

  case object INDEPENDENT extends CustomerGenerationStrategy {
    override def name: String = "independent"
  }

  case object CORRELATED extends CustomerGenerationStrategy {
    override def name: String = "correlated"
  }

  val allStrategies: Seq[CustomerGenerationStrategy] =
    Seq(INDEPENDENT, CORRELATED)

  def fromString(
                  strategy: String
                ): Either[String, CustomerGenerationStrategy] = {

    allStrategies.find(_.name == strategy.trim.toLowerCase) match {
      case Some(value) => Right(value)
      case None =>
        Left(s"Unsupported customer generation strategy: $strategy")
    }
  }
}