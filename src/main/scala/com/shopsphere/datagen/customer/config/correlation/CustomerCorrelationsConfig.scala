package com.shopsphere.datagen.customer.config.correlation

case class CustomerCorrelationsConfig(
                                       segmentByAgeBand: Seq[AgeBandSegmentDistributionConfig],
                                       preferredDeviceByAgeBand: Seq[AgeBandDeviceDistributionConfig],
                                       preferredPaymentMethodBySegment: Seq[SegmentPaymentMethodDistributionConfig]
                                     )