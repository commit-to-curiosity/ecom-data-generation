package com.shopsphere.datagen.customer.loader

import com.shopsphere.datagen.common.config.ConfigLoader
import com.shopsphere.datagen.common.enums.PreferredPaymentMethod
import com.shopsphere.datagen.customer.config.behavior.CustomerPreferredPaymentMethodConfig
import com.typesafe.config.Config

class CustomerPreferredPaymentMethodLoader
  extends ConfigLoader[CustomerPreferredPaymentMethodConfig] {

  override def loadConfiguration(
                                  config: Config
                                ): CustomerPreferredPaymentMethodConfig = {
    CustomerPreferredPaymentMethodConfig(
      upi = config.getDouble(PreferredPaymentMethod.UPI.name),
      creditCard = config.getDouble(PreferredPaymentMethod.CREDIT_CARD.name),
      debitCard = config.getDouble(PreferredPaymentMethod.DEBIT_CARD.name),
      netBanking = config.getDouble(PreferredPaymentMethod.NET_BANKING.name),
      wallet = config.getDouble(PreferredPaymentMethod.WALLET.name)
    )
  }
}