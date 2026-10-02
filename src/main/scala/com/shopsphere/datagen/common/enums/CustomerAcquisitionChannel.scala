package com.shopsphere.datagen.common.enums

sealed trait CustomerAcquisitionValue {
  def name: String
  def path: String
}

sealed trait CustomerAcquisitionChannel extends CustomerAcquisitionValue {
  override def path: String = name
}

sealed trait CustomerAcquisitionCampaign extends CustomerAcquisitionValue {
  def channel: CustomerAcquisitionChannel
  override def path: String = s"${channel.name}.$name"
}

object CustomerAcquisitionChannel {

  case object ORGANIC extends CustomerAcquisitionChannel {
    override val name: String = "organic"

    case object SEO extends CustomerAcquisitionCampaign {
      override val name: String = "seo"
      override val channel: CustomerAcquisitionChannel = ORGANIC
    }

    case object CONTENT extends CustomerAcquisitionCampaign {
      override val name: String = "content"
      override val channel: CustomerAcquisitionChannel = ORGANIC
    }
  }

  case object PAID_SEARCH extends CustomerAcquisitionChannel {
    override val name: String = "paid_search"

    case object BRAND extends CustomerAcquisitionCampaign {
      override val name: String = "brand"
      override val channel: CustomerAcquisitionChannel = PAID_SEARCH
    }

    case object GENERIC extends CustomerAcquisitionCampaign {
      override val name: String = "generic"
      override val channel: CustomerAcquisitionChannel = PAID_SEARCH
    }
  }

  case object SOCIAL extends CustomerAcquisitionChannel {
    override val name: String = "social"

    case object INSTAGRAM extends CustomerAcquisitionCampaign {
      override val name: String = "instagram"
      override val channel: CustomerAcquisitionChannel = SOCIAL
    }

    case object FACEBOOK extends CustomerAcquisitionCampaign {
      override val name: String = "facebook"
      override val channel: CustomerAcquisitionChannel = SOCIAL
    }

    case object YOUTUBE extends CustomerAcquisitionCampaign {
      override val name: String = "youtube"
      override val channel: CustomerAcquisitionChannel = SOCIAL
    }
  }

  case object EMAIL extends CustomerAcquisitionChannel {
    override val name: String = "email"

    case object NEWSLETTER extends CustomerAcquisitionCampaign {
      override val name: String = "newsletter"
      override val channel: CustomerAcquisitionChannel = EMAIL
    }

    case object PROMOTION extends CustomerAcquisitionCampaign {
      override val name: String = "promotion"
      override val channel: CustomerAcquisitionChannel = EMAIL
    }
  }

  case object DIRECT extends CustomerAcquisitionChannel {
    override val name: String = "direct"

    case object NONE extends CustomerAcquisitionCampaign {
      override val name: String = "none"
      override val channel: CustomerAcquisitionChannel = DIRECT
    }
  }

  case object REFERRAL extends CustomerAcquisitionChannel {
    override val name: String = "referral"

    case object CUSTOMER_REFERRAL extends CustomerAcquisitionCampaign {
      override val name: String = "customer_referral"
      override val channel: CustomerAcquisitionChannel = REFERRAL
    }
  }

  val allChannels: Seq[CustomerAcquisitionChannel] =
    Seq(
      ORGANIC,
      PAID_SEARCH,
      SOCIAL,
      EMAIL,
      DIRECT,
      REFERRAL
    )

  def fromString(
                  channelString: String
                ): Either[String, CustomerAcquisitionChannel] = {
    allChannels.find(_.name == channelString) match {
      case Some(channel) => Right(channel)
      case None =>
        Left(
          s"Unsupported customer acquisition channel: $channelString"
        )
    }
  }
}