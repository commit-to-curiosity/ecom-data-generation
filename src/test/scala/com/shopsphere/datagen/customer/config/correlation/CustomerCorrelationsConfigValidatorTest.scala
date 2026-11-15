
package com.shopsphere.datagen.customer.config.correlation

import com.shopsphere.datagen.common.enums.CustomerSegment
import com.shopsphere.datagen.customer.config.{
  CustomerAgeBandConfig,
  CustomerSegmentConfig
}
import org.scalatest.funsuite.AnyFunSuite

class CustomerCorrelationsConfigValidatorTest extends AnyFunSuite {

  private val ageBands = Seq(
    CustomerAgeBandConfig(18, 24, 0.10),
    CustomerAgeBandConfig(25, 34, 0.25),
    CustomerAgeBandConfig(35, 44, 0.30),
    CustomerAgeBandConfig(45, 54, 0.20),
    CustomerAgeBandConfig(55, 64, 0.10),
    CustomerAgeBandConfig(65, 80, 0.05)
  )

  private val segmentConfig =
    CustomerSegmentConfig(
      standard = 0.70,
      premium = 0.20,
      vip = 0.08,
      business = 0.02
    )

  private val correlations = CustomerCorrelationsConfig(
    segmentByAgeBand = ageBands.map { band =>
      AgeBandSegmentDistributionConfig(
        band.minAge,
        band.maxAge,
        standard = 0.70,
        premium = 0.20,
        vip = 0.08,
        business = 0.02
      )
    },
    preferredDeviceByAgeBand = ageBands.map { band =>
      AgeBandDeviceDistributionConfig(
        band.minAge,
        band.maxAge,
        mobile = 0.70,
        desktop = 0.25,
        tablet = 0.05
      )
    },
    preferredPaymentMethodBySegment =
      CustomerSegment.allSegments.map { segment =>
        SegmentPaymentMethodDistributionConfig(
          segment,
          upi = 0.45,
          creditCard = 0.20,
          debitCard = 0.15,
          netBanking = 0.10,
          wallet = 0.10
        )
      }
  )

  private def validate(
                        config: CustomerCorrelationsConfig,
                        configuredAgeBands: Seq[CustomerAgeBandConfig] = ageBands
                      ): Unit =
    CustomerCorrelationsConfigValidator.validate(
      config,
      configuredAgeBands,
      segmentConfig
    )

  test("should accept valid correlation configuration") {
    validate(correlations)
  }

  test("should reject duplicate segment age bands") {
    val invalid = correlations.copy(
      segmentByAgeBand =
        correlations.segmentByAgeBand.updated(
          1,
          correlations.segmentByAgeBand.head
        )
    )

    intercept[IllegalArgumentException] {
      validate(invalid)
    }
  }

  test("should reject missing device age bands") {
    val invalid = correlations.copy(
      preferredDeviceByAgeBand =
        correlations.preferredDeviceByAgeBand.dropRight(1)
    )

    intercept[IllegalArgumentException] {
      validate(invalid)
    }
  }

  test("should reject invalid conditional probabilities") {
    val invalid = correlations.copy(
      segmentByAgeBand =
        correlations.segmentByAgeBand.updated(
          0,
          correlations.segmentByAgeBand.head.copy(
            standard = Double.NaN
          )
        )
    )

    intercept[IllegalArgumentException] {
      validate(invalid)
    }
  }

  test("should reject missing payment-method segment distributions") {
    val invalid = correlations.copy(
      preferredPaymentMethodBySegment =
        correlations.preferredPaymentMethodBySegment.dropRight(1)
    )

    intercept[IllegalArgumentException] {
      validate(invalid)
    }
  }

  test("should reject aggregate segment proportion mismatch") {
    val firstBand = correlations.segmentByAgeBand.head

    val invalid = correlations.copy(
      segmentByAgeBand =
        correlations.segmentByAgeBand.updated(
          0,
          firstBand.copy(
            standard = 0.80,
            premium = 0.10
          )
        )
    )

    intercept[IllegalArgumentException] {
      validate(invalid)
    }
  }

  test("should reject overlapping demographic age bands") {
    val overlappingAgeBands = Seq(
      CustomerAgeBandConfig(18, 24, 0.10),
      CustomerAgeBandConfig(23, 34, 0.25),
      CustomerAgeBandConfig(35, 44, 0.30),
      CustomerAgeBandConfig(45, 54, 0.20),
      CustomerAgeBandConfig(55, 64, 0.10),
      CustomerAgeBandConfig(65, 80, 0.05)
    )

    intercept[IllegalArgumentException] {
      validate(correlations, overlappingAgeBands)
    }
  }

  test("should reject gaps between demographic age bands") {
    val ageBandsWithGap = Seq(
      CustomerAgeBandConfig(18, 24, 0.10),
      CustomerAgeBandConfig(26, 34, 0.25),
      CustomerAgeBandConfig(35, 44, 0.30),
      CustomerAgeBandConfig(45, 54, 0.20),
      CustomerAgeBandConfig(55, 64, 0.10),
      CustomerAgeBandConfig(65, 80, 0.05)
    )

    intercept[IllegalArgumentException] {
      validate(correlations, ageBandsWithGap)
    }
  }
}
