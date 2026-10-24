
package com.shopsphere.datagen.scenario

import com.shopsphere.datagen.product.model.Product
import com.shopsphere.datagen.product.reference.ProductReferenceData
import org.scalatest.funsuite.AnyFunSuite

class GenerationScenarioLoaderTest extends AnyFunSuite {

  private val products =
    (1 to 10).map { number =>
      val id = f"PROD_$number%03d"

      Product(
        id = id,
        name = s"Product $number",
        category = "test",
        brand = "TestBrand",
        price = BigDecimal("100.00")
      )
    }

  private val loader =
    new GenerationScenarioLoader(
      new ProductReferenceData(products)
    )

  test("default scenario should be baseline") {
    val scenario = loader.loadDefault()

    assert(scenario.name == "baseline")
    assert(scenario.ordersPerCustomer == 2)
    assert(scenario.itemsPerOrder == 2)
    assert(
      scenario.productSelection.strategy ==
        ProductSelectionStrategy.Uniform
    )
  }

  test("high demand scenario should increase order and item counts") {
    val scenario = loader.load("high_demand")

    assert(scenario.ordersPerCustomer == 4)
    assert(scenario.itemsPerOrder == 4)
    assert(
      scenario.productSelection.strategy ==
        ProductSelectionStrategy.Weighted
    )
    assert(scenario.productSelection.weights.nonEmpty)
    assert(
      scenario.productSelection.weights.keySet ==
        products.map(_.id).toSet
    )
  }

  test("payment failures scenario should use increased failure weights") {
    val scenario = loader.load("payment_failures")
    val weights = scenario.paymentOutcomeWeights

    assert(scenario.ordersPerCustomer == 2)
    assert(scenario.itemsPerOrder == 2)
    assert(weights.successOnFirstAttempt == 0.45)
    assert(weights.successAfterOneRetry == 0.25)
    assert(weights.successAfterTwoRetries == 0.15)
    assert(weights.permanentFailure == 0.15)
  }

  test("unknown scenario should be rejected") {
    assertThrows[IllegalArgumentException] {
      loader.load("unknown_scenario")
    }
  }

  test("weighted selection should reject empty weights") {
    assertThrows[IllegalArgumentException] {
      ProductSelectionConfig(
        strategy = ProductSelectionStrategy.Weighted,
        weights = Map.empty
      )
    }
  }

  test("uniform selection should reject configured weights") {
    assertThrows[IllegalArgumentException] {
      ProductSelectionConfig(
        strategy = ProductSelectionStrategy.Uniform,
        weights = Map("PROD_001" -> 1.0)
      )
    }
  }

  test("negative order and item counts should be rejected") {
    val selection = ProductSelectionConfig(
      strategy = ProductSelectionStrategy.Uniform,
      weights = Map.empty
    )

    val paymentWeights = PaymentOutcomeWeights(
      successOnFirstAttempt = 0.70,
      successAfterOneRetry = 0.20,
      successAfterTwoRetries = 0.07,
      permanentFailure = 0.03
    )

    assertThrows[IllegalArgumentException] {
      GenerationScenario(
        name = "invalid",
        ordersPerCustomer = -1,
        itemsPerOrder = 2,
        productSelection = selection,
        paymentOutcomeWeights = paymentWeights
      )
    }

    assertThrows[IllegalArgumentException] {
      GenerationScenario(
        name = "invalid",
        ordersPerCustomer = 2,
        itemsPerOrder = -1,
        productSelection = selection,
        paymentOutcomeWeights = paymentWeights
      )
    }
  }

  test("invalid payment weights should be rejected") {
    assertThrows[IllegalArgumentException] {
      PaymentOutcomeWeights(
        successOnFirstAttempt = 0.70,
        successAfterOneRetry = -0.20,
        successAfterTwoRetries = 0.07,
        permanentFailure = 0.03
      )
    }

    assertThrows[IllegalArgumentException] {
      PaymentOutcomeWeights(
        successOnFirstAttempt = 0.0,
        successAfterOneRetry = 0.0,
        successAfterTwoRetries = 0.0,
        permanentFailure = 0.0
      )
    }
  }
}
