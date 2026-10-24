package com.shopsphere.datagen.scenario

final case class PaymentOutcomeWeights(
                                        successOnFirstAttempt: Double,
                                        successAfterOneRetry: Double,
                                        successAfterTwoRetries: Double,
                                        permanentFailure: Double
                                      ) {
  val all: Seq[(String, Double)] = Seq(
    "SUCCESS_ON_FIRST_ATTEMPT" -> successOnFirstAttempt,
    "SUCCESS_AFTER_ONE_RETRY" -> successAfterOneRetry,
    "SUCCESS_AFTER_TWO_RETRIES" -> successAfterTwoRetries,
    "PERMANENT_FAILURE" -> permanentFailure
  )

  require(
    all.forall { case (_, weight) =>
      !weight.isNaN && !weight.isInfinity && weight >= 0.0
    },
    "Payment outcome weights must be finite and non-negative"
  )

  require(
    all.exists { case (_, weight) => weight > 0.0 },
    "At least one payment outcome weight must be positive"
  )
}

sealed trait ProductSelectionStrategy

object ProductSelectionStrategy {
  case object Uniform extends ProductSelectionStrategy
  case object Weighted extends ProductSelectionStrategy

  def fromString(value: String): ProductSelectionStrategy =
    value.trim.toLowerCase match {
      case "uniform"  => Uniform
      case "weighted" => Weighted
      case other =>
        throw new IllegalArgumentException(
          s"Unsupported product selection strategy: '$other'"
        )
    }
}

final case class ProductSelectionConfig(
                                         strategy: ProductSelectionStrategy,
                                         weights: Map[String, Double]
                                       ) {
  require(
    weights.values.forall { weight =>
      !weight.isNaN && !weight.isInfinity && weight >= 0.0
    },
    "Product selection weights must be finite and non-negative"
  )

  strategy match {
    case ProductSelectionStrategy.Uniform =>
      require(
        weights.isEmpty,
        "Uniform product selection must not define product weights"
      )

    case ProductSelectionStrategy.Weighted =>
      require(
        weights.nonEmpty,
        "Weighted product selection requires product weights"
      )
      require(
        weights.values.exists(_ > 0.0),
        "At least one product weight must be positive"
      )
  }
}

final case class GenerationScenario(
                                     name: String,
                                     ordersPerCustomer: Int,
                                     itemsPerOrder: Int,
                                     productSelection: ProductSelectionConfig,
                                     paymentOutcomeWeights: PaymentOutcomeWeights
                                   ) {
  require(name.trim.nonEmpty, "Scenario name must not be empty")
  require(ordersPerCustomer >= 0, "Orders per customer must not be negative")
  require(itemsPerOrder >= 0, "Items per order must not be negative")
}