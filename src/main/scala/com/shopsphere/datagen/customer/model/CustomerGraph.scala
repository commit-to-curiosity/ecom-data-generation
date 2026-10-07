package com.shopsphere.datagen.customer.model

import com.shopsphere.datagen.address.model.Address
import com.shopsphere.datagen.order.model.{Order, OrderItem}
import com.shopsphere.datagen.payment.model.Payment

case class CustomerGraph(
                          customer: Customer,
                          addresses: Seq[Address],
                          orders: Seq[Order],
                          orderItems: Seq[OrderItem],
                          payments: Seq[Payment]
                        )