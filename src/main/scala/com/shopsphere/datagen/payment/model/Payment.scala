package com.shopsphere.datagen.payment.model

import java.time.LocalDate

case class Payment(
                    id: String,
                    orderId: String,
                    paymentDate: LocalDate,
                    paymentMethod: String,
                    paymentStatus: String,
                    amount: BigDecimal
                  )