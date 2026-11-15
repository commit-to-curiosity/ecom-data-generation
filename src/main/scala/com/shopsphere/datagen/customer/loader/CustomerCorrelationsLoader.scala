package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.{ConfigLoader, DataGenerationConstants}
import com.shopsphere.datagen.common.enums.CustomerSegment
import com.shopsphere.datagen.customer.config.correlation._
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

class CustomerCorrelationsLoader
  extends ConfigLoader[CustomerCorrelationsConfig] {

  override def loadConfiguration(
                                  config: Config
                                ): CustomerCorrelationsConfig = {

    CustomerCorrelationsConfig(
      segmentByAgeBand =
        config.getConfigList(DataGenerationConstants.SEGMENT_BY_AGE_BAND)
          .asScala
          .map { entry =>
            AgeBandSegmentDistributionConfig(
              minAge = entry.getInt(DataGenerationConstants.MIN_AGE),
              maxAge = entry.getInt(DataGenerationConstants.MAX_AGE),
              standard = entry.getDouble(CustomerSegment.STANDARD.name),
              premium = entry.getDouble(CustomerSegment.PREMIUM.name),
              vip = entry.getDouble(CustomerSegment.VIP.name),
              business = entry.getDouble(CustomerSegment.BUSINESS.name)
            )
          }.toSeq,

      preferredDeviceByAgeBand =
        config.getConfigList(DataGenerationConstants.PREFERRED_DEVICE_BY_AGE_BAND)
          .asScala
          .map { entry =>
            AgeBandDeviceDistributionConfig(
              minAge = entry.getInt(DataGenerationConstants.MIN_AGE),
              maxAge = entry.getInt(DataGenerationConstants.MAX_AGE),
              mobile = entry.getDouble("mobile"),
              desktop = entry.getDouble("desktop"),
              tablet = entry.getDouble("tablet")
            )
          }.toSeq,

      preferredPaymentMethodBySegment =
        config.getConfigList(
            DataGenerationConstants.PREFERRED_PAYMENT_METHOD_BY_SEGMENT
          ).asScala
          .map { entry =>
            val segmentName = entry.getString(DataGenerationConstants.SEGMENT)

            val segment =
              CustomerSegment.fromString(segmentName.trim.toLowerCase) match {
                case Right(value) => value
                case Left(error) => throw new IllegalArgumentException(error)
              }

            SegmentPaymentMethodDistributionConfig(
              segment = segment,
              upi = entry.getDouble("upi"),
              creditCard = entry.getDouble("credit_card"),
              debitCard = entry.getDouble("debit_card"),
              netBanking = entry.getDouble("net_banking"),
              wallet = entry.getDouble("wallet")
            )
          }.toSeq
    )
  }
}