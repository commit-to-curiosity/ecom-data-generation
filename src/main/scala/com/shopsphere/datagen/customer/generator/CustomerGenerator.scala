package com.shopsphere.datagen.customer.generator

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.shopsphere.datagen.common.distribution.{RandomGenerator, UniformDistribution, WeightedDistribution}
import com.shopsphere.datagen.common.generator.Generator
import com.shopsphere.datagen.customer.config.CustomerAgeBandConfig
import com.shopsphere.datagen.customer.config.CustomerConfig
import com.shopsphere.datagen.customer.model.Customer
import com.shopsphere.datagen.common.enums.{CustomerAcquisitionCampaign, CustomerAcquisitionChannel, CustomerSegment, CustomerStatus, Gender, PreferredDevice, PreferredPaymentMethod}

class CustomerGenerator(
                         customerConfig: CustomerConfig,
                         randomGenerator: RandomGenerator,
                         identityGenerator: CustomerIdentityGenerator
                       ) extends Generator[Customer] {

  private var nextCustomerId = 1L

  private val genderDistribution =
    new WeightedDistribution[Gender](
      Seq(
        Gender.MALE -> customerConfig.demographics.gender.male,
        Gender.FEMALE -> customerConfig.demographics.gender.female,
        Gender.OTHER -> customerConfig.demographics.gender.other
      ),
      randomGenerator
    )

  private val ageBandDistribution =
    new WeightedDistribution[CustomerAgeBandConfig](
      customerConfig.demographics.ageBands.map(ageBand =>
        ageBand -> ageBand.weight
      ),
      randomGenerator
    )

  private val customerStatusDistribution =
    new WeightedDistribution[CustomerStatus](
      Seq(
        CustomerStatus.ACTIVE -> customerConfig.status.active,
        CustomerStatus.INACTIVE -> customerConfig.status.inactive,
        CustomerStatus.SUSPENDED -> customerConfig.status.suspended,
        CustomerStatus.CLOSED -> customerConfig.status.closed
      ),
      randomGenerator
    )

  private val customerSegmentDistribution =
    new WeightedDistribution[CustomerSegment](
      Seq(
        CustomerSegment.STANDARD -> customerConfig.segment.standard,
        CustomerSegment.PREMIUM -> customerConfig.segment.premium,
        CustomerSegment.VIP -> customerConfig.segment.vip,
        CustomerSegment.BUSINESS -> customerConfig.segment.business
      ),
      randomGenerator
    )

  private val acquisitionChannelDistribution =
    new WeightedDistribution[CustomerAcquisitionChannel](
      Seq(
        CustomerAcquisitionChannel.ORGANIC ->
          customerConfig.acquisition.channel.organic,
        CustomerAcquisitionChannel.PAID_SEARCH ->
          customerConfig.acquisition.channel.paidSearch,
        CustomerAcquisitionChannel.SOCIAL ->
          customerConfig.acquisition.channel.social,
        CustomerAcquisitionChannel.EMAIL ->
          customerConfig.acquisition.channel.email,
        CustomerAcquisitionChannel.DIRECT ->
          customerConfig.acquisition.channel.direct,
        CustomerAcquisitionChannel.REFERRAL ->
          customerConfig.acquisition.channel.referral
      ),
      randomGenerator
    )

  private val preferredDeviceDistribution =
    new WeightedDistribution[PreferredDevice](
      Seq(
        PreferredDevice.MOBILE ->
          customerConfig.behavior.preferredDevice.mobile,
        PreferredDevice.DESKTOP ->
          customerConfig.behavior.preferredDevice.desktop,
        PreferredDevice.TABLET ->
          customerConfig.behavior.preferredDevice.tablet
      ),
      randomGenerator
    )

  private val preferredPaymentMethodDistribution =
    new WeightedDistribution[PreferredPaymentMethod](
      Seq(
        PreferredPaymentMethod.UPI ->
          customerConfig.behavior.preferredPaymentMethod.upi,
        PreferredPaymentMethod.CREDIT_CARD ->
          customerConfig.behavior.preferredPaymentMethod.creditCard,
        PreferredPaymentMethod.DEBIT_CARD ->
          customerConfig.behavior.preferredPaymentMethod.debitCard,
        PreferredPaymentMethod.NET_BANKING ->
          customerConfig.behavior.preferredPaymentMethod.netBanking,
        PreferredPaymentMethod.WALLET ->
          customerConfig.behavior.preferredPaymentMethod.wallet
      ),
      randomGenerator
    )

  override def generate(): Customer = {
    val customerId = generateCustomerId()
    val firstName = identityGenerator.generateFirstName()
    val lastName = identityGenerator.generateLastName()
    val email = identityGenerator.generateEmail(firstName, lastName)
    val phone = identityGenerator.generatePhone()
    val gender = genderDistribution.sample()
    val ageBand = ageBandDistribution.sample()
    val age = generateAge(ageBand)

    val asOfDate = customerConfig.lifecycle.asOfDate
    val dateOfBirth = generateDateOfBirth(age)
    val registrationDate = generateRegistrationDate(asOfDate)

    val customerStatus = customerStatusDistribution.sample()
    val customerSegment = customerSegmentDistribution.sample()
    val acquisitionChannel = acquisitionChannelDistribution.sample()
    val acquisitionCampaign = generateAcquisitionCampaign(acquisitionChannel)
    val preferredDevice = preferredDeviceDistribution.sample()
    val preferredPaymentMethod = preferredPaymentMethodDistribution.sample()

    Customer(
      id = customerId,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      gender = gender.name,
      dateOfBirth = dateOfBirth,
      registrationDate = registrationDate,
      customerStatus = customerStatus.name,
      customerSegment = customerSegment.name,
      acquisitionChannel = acquisitionChannel.name,
      acquisitionCampaign = acquisitionCampaign.name,
      preferredDevice = preferredDevice.name,
      preferredPaymentMethod = preferredPaymentMethod.name
    )
  }

  private def generateCustomerId(): String = {
    val customerId = f"CUST_$nextCustomerId%06d"
    nextCustomerId += 1
    customerId
  }

  private def generateAge(
                           ageBand: CustomerAgeBandConfig
                         ): Int = {
    val ageDistribution = new UniformDistribution[Int](
      ageBand.minAge to ageBand.maxAge,
      randomGenerator
    )

    ageDistribution.sample()
  }

  private def generateDateOfBirth(age: Int): LocalDate = {
    val asOfDate = customerConfig.lifecycle.asOfDate
    val latestBirthDate = asOfDate.minusYears(age.toLong)
    val earliestBirthDate =
      latestBirthDate.minusYears(1).plusDays(1)

    val birthDateRange =
      ChronoUnit.DAYS.between(
        earliestBirthDate,
        latestBirthDate
      ).toInt

    val birthDateDistribution = new UniformDistribution[Int](
      0 to birthDateRange,
      randomGenerator
    )

    earliestBirthDate.plusDays(
      birthDateDistribution.sample().toLong
    )
  }

  private def generateRegistrationDate(
                                        asOfDate: LocalDate
                                      ): LocalDate = {
    val registrationHistoryDays =
      customerConfig.lifecycle.registrationHistoryDays

    val earliestRegistrationDate =
      asOfDate.minusDays(registrationHistoryDays.toLong)

    val registrationDayDistribution = new UniformDistribution[Int](
      0 to registrationHistoryDays,
      randomGenerator
    )

    earliestRegistrationDate.plusDays(
      registrationDayDistribution.sample().toLong
    )
  }

  private def generateAcquisitionCampaign(
                                           channel: CustomerAcquisitionChannel
                                         ): CustomerAcquisitionCampaign = {
    channel match {
      case CustomerAcquisitionChannel.ORGANIC =>
        new WeightedDistribution[CustomerAcquisitionCampaign](
          Seq(
            CustomerAcquisitionChannel.ORGANIC.SEO ->
              customerConfig.acquisition.campaign.organic.seo,
            CustomerAcquisitionChannel.ORGANIC.CONTENT ->
              customerConfig.acquisition.campaign.organic.content
          ),
          randomGenerator
        ).sample()

      case CustomerAcquisitionChannel.PAID_SEARCH =>
        new WeightedDistribution[CustomerAcquisitionCampaign](
          Seq(
            CustomerAcquisitionChannel.PAID_SEARCH.BRAND ->
              customerConfig.acquisition.campaign.paidSearch.brand,
            CustomerAcquisitionChannel.PAID_SEARCH.GENERIC ->
              customerConfig.acquisition.campaign.paidSearch.generic
          ),
          randomGenerator
        ).sample()

      case CustomerAcquisitionChannel.SOCIAL =>
        new WeightedDistribution[CustomerAcquisitionCampaign](
          Seq(
            CustomerAcquisitionChannel.SOCIAL.INSTAGRAM ->
              customerConfig.acquisition.campaign.social.instagram,
            CustomerAcquisitionChannel.SOCIAL.FACEBOOK ->
              customerConfig.acquisition.campaign.social.facebook,
            CustomerAcquisitionChannel.SOCIAL.YOUTUBE ->
              customerConfig.acquisition.campaign.social.youtube
          ),
          randomGenerator
        ).sample()

      case CustomerAcquisitionChannel.EMAIL =>
        new WeightedDistribution[CustomerAcquisitionCampaign](
          Seq(
            CustomerAcquisitionChannel.EMAIL.NEWSLETTER ->
              customerConfig.acquisition.campaign.email.newsletter,
            CustomerAcquisitionChannel.EMAIL.PROMOTION ->
              customerConfig.acquisition.campaign.email.promotion
          ),
          randomGenerator
        ).sample()

      case CustomerAcquisitionChannel.DIRECT =>
        CustomerAcquisitionChannel.DIRECT.NONE

      case CustomerAcquisitionChannel.REFERRAL =>
        CustomerAcquisitionChannel.REFERRAL.CUSTOMER_REFERRAL
    }
  }
}