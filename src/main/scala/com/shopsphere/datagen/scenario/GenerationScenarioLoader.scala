package com.shopsphere.datagen.scenario

import com.shopsphere.datagen.product.reference.ProductReferenceData
import com.typesafe.config.{Config, ConfigFactory}
import scala.jdk.CollectionConverters._

final class GenerationScenarioLoader(
                                      productReferenceData: ProductReferenceData
                                    ) {

  def loadDefault(): GenerationScenario = {
    val config =
      ConfigFactory.parseResources("scenario.conf").resolve()

    val scenarioName =
      config.getString("scenarios.default")

    load(config, scenarioName)
  }

  def load(scenarioName: String): GenerationScenario = {
    val config =
      ConfigFactory.parseResources("scenario.conf").resolve()

    load(config, scenarioName)
  }

  private def load(
                    config: Config,
                    scenarioName: String
                  ): GenerationScenario = {
    val scenarios = config.getConfig("scenarios")

    require(
      scenarios.hasPath(scenarioName),
      s"Unknown generation scenario '$scenarioName'"
    )

    val scenario = scenarios.getConfig(scenarioName)

    val productConfig =
      scenario.getConfig("product_selection")

    val strategy =
      ProductSelectionStrategy.fromString(
        productConfig.getString("strategy")
      )

    val weightsConfig =
      productConfig.getConfig("weights")

    val productWeights =
      weightsConfig.root().keySet().asScala.iterator.map { productId =>
        productId -> weightsConfig.getDouble(productId)
      }.toMap

    validateProductWeights(strategy, productWeights)

    val paymentConfig =
      scenario.getConfig("payment_outcomes")

    GenerationScenario(
      name = scenarioName,
      ordersPerCustomer = scenario.getInt("orders_per_customer"),
      itemsPerOrder = scenario.getInt("items_per_order"),
      productSelection = ProductSelectionConfig(
        strategy = strategy,
        weights = productWeights
      ),
      paymentOutcomeWeights = PaymentOutcomeWeights(
        successOnFirstAttempt =
          paymentConfig.getDouble("success_on_first_attempt"),
        successAfterOneRetry =
          paymentConfig.getDouble("success_after_one_retry"),
        successAfterTwoRetries =
          paymentConfig.getDouble("success_after_two_retries"),
        permanentFailure =
          paymentConfig.getDouble("permanent_failure")
      )
    )
  }

  private def validateProductWeights(
                                      strategy: ProductSelectionStrategy,
                                      weights: Map[String, Double]
                                    ): Unit = {
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

        val unknownProducts =
          weights.keySet -- productReferenceData.productsById.keySet

        require(
          unknownProducts.isEmpty,
          s"Unknown product IDs in scenario weights: ${unknownProducts.toSeq.sorted.mkString(", ")}"
        )

        require(
          weights.values.exists(_ > 0.0),
          "At least one product weight must be positive"
        )
    }
  }
}