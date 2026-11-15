
package com.shopsphere.datagen.common.analysis

final case class CategoryFrequency(
                                    value: String,
                                    count: Long,
                                    proportion: Double
                                  )

final case class CategoryDistribution(
                                       total: Long,
                                       categories: Vector[CategoryFrequency]
                                     )

final case class AgeBandFrequency(
                                   minAge: Int,
                                   maxAge: Int,
                                   count: Long,
                                   proportion: Double
                                 )

final case class AgeStatistics(
                                minAge: Option[Int],
                                maxAge: Option[Int],
                                meanAge: Option[Double],
                                medianAge: Option[Double],
                                ageBands: Vector[AgeBandFrequency]
                              )

final case class ChannelCampaignDistribution(
                                              channel: String,
                                              totalCustomers: Long,
                                              campaigns: Vector[CategoryFrequency]
                                            )

final case class CustomerProfileSummary(
                                         customerCount: Long,
                                         age: AgeStatistics,
                                         gender: CategoryDistribution,
                                         customerStatus: CategoryDistribution,
                                         customerSegment: CategoryDistribution,
                                         acquisitionChannel: CategoryDistribution,
                                         acquisitionCampaignByChannel: Vector[ChannelCampaignDistribution],
                                         preferredDevice: CategoryDistribution,
                                         preferredPaymentMethod: CategoryDistribution
                                       )
