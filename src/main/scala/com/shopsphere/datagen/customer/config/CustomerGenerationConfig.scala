package com.shopsphere.datagen.customer.config

import com.shopsphere.datagen.customer.config.correlation.CustomerCorrelationsConfig

case class CustomerGenerationConfig(
                                     strategy: CustomerGenerationStrategy =
                                     CustomerGenerationStrategy.INDEPENDENT,
                                     correlations: Option[CustomerCorrelationsConfig] = None
                                   )