package com.shopsphere.datagen.customer.config

import com.shopsphere.datagen.customer.config.behavior.{CustomerPreferredDeviceConfig, CustomerPreferredPaymentMethodConfig}

case class CustomerBehaviorConfig(
                                   preferredDevice: CustomerPreferredDeviceConfig,
                                   preferredPaymentMethod: CustomerPreferredPaymentMethodConfig
                                 )