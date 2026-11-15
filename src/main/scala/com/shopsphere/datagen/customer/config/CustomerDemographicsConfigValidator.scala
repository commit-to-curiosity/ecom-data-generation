package com.shopsphere.datagen.customer.config

object CustomerDemographicsConfigValidator {

  private val TOLERANCE = 0.000001

  def validate(ageBands: Seq[CustomerAgeBandConfig]): Unit = {
    require(ageBands.nonEmpty, "Age bands must not be empty")

    ageBands.foreach { band =>
      require(
        band.minAge >= 0,
        s"Minimum age must not be negative: ${band.minAge}"
      )

      require(
        band.minAge <= band.maxAge,
        s"Invalid age range: ${band.minAge}-${band.maxAge}"
      )

      require(
        !band.weight.isNaN &&
          !band.weight.isInfinity &&
          band.weight >= 0.0,
        s"Invalid population weight for age band " +
          s"${band.minAge}-${band.maxAge}: ${band.weight}"
      )
    }

    val totalWeight = ageBands.map(_.weight).sum

    require(
      !totalWeight.isNaN &&
        !totalWeight.isInfinity &&
        math.abs(totalWeight - 1.0) <= TOLERANCE,
      s"Age band population weights must sum to 1.0; actual total: $totalWeight"
    )

    validateContiguousAgeBands(ageBands)
  }

  private def validateContiguousAgeBands(
                                          ageBands: Seq[CustomerAgeBandConfig]
                                        ): Unit = {

    val sortedAgeBands = ageBands.sortBy(_.minAge)

    sortedAgeBands.sliding(2).foreach {
      case Seq(previous, current) =>
        require(
          current.minAge.toLong == previous.maxAge.toLong + 1L,
          s"Age bands must be contiguous and non-overlapping: " +
            s"${previous.minAge}-${previous.maxAge} followed by " +
            s"${current.minAge}-${current.maxAge}"
        )

      case _ =>
    }
  }
}
