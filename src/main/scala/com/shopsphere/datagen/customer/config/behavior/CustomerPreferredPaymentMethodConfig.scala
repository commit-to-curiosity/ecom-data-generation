package com.shopsphere.datagen.customer.config.behavior

case class CustomerPreferredPaymentMethodConfig(
                                                 upi: Double,
                                                 creditCard: Double,
                                                 debitCard: Double,
                                                 netBanking: Double,
                                                 wallet: Double
                                               )