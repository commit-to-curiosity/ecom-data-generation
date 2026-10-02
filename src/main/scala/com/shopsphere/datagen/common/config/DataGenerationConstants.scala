package com.shopsphere.datagen.common.config

object DataGenerationConstants {

  // Age band configuration keys
  val AGE_BANDS = "age_bands"
  val MAX_AGE   = "max_age"
  val MIN_AGE   = "min_age"
  val WEIGHT    = "weight"

  // Configuration files
  val CUSTOMER_CONF_FILE = "customer.conf"
  val CUSTOMER_JSON_FILE = "customer.json"
  val CUSTOMER_YAML_FILE = "customer.yaml"

  // Configuration sections
  val CUSTOMER     = "customer"
  val DEMOGRAPHICS = "demographics"
  val GENDER       = "gender"
  val LIFECYCLE    = "lifecycle"
  val SEGMENT      = "segment"
  val STATUS       = "status"

  // Lifecycle configuration keys
  val AS_OF_DATE                  = "as_of_date"
  val REGISTRATION_HISTORY_DAYS   = "registration_history_days"

  // Acquisition configuration sections
  val ACQUISITION = "acquisition"
  val CAMPAIGN = "campaign"
  val CHANNEL = "channel"

  // Behavioral configuration sections
  val BEHAVIOR = "behavior"
  val PREFERRED_DEVICE = "preferred_device"
  val PREFERRED_PAYMENT_METHOD = "preferred_payment_method"

}