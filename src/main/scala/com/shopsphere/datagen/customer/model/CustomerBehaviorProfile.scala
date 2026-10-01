package com.shopsphere.datagen.customer.model

import com.shopsphere.datagen.device.DeviceType
import com.shopsphere.datagen.payment.PaymentMethod

case class CustomerBehaviorProfile(
                                    preferredDevice: DeviceType,
                                    preferredPaymentMethod: PaymentMethod
                                  )