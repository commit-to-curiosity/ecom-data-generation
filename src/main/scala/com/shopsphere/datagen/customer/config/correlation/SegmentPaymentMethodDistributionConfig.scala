package com.shopsphere.datagen.customer.config.correlation

import com.shopsphere.datagen.common.enums.CustomerSegment

case class SegmentPaymentMethodDistributionConfig(
                                                   segment: CustomerSegment,
                                                   upi: Double,
                                                   creditCard: Double,
                                                   debitCard: Double,
                                                   netBanking: Double,
                                                   wallet: Double
                                                 )