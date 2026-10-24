
package com.shopsphere.datagen.common.json

import com.shopsphere.datagen.customer.model.Customer
import com.fasterxml.jackson.databind.JsonNode
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class JsonMapperTest extends AnyFunSuite {

  private def sampleCustomer: Customer =
    Customer(
      id = "CUST000001",
      firstName = "Asha",
      lastName = "Patel",
      email = "asha@example.com",
      phone = "9876543210",
      gender = "FEMALE",
      dateOfBirth = LocalDate.parse("1990-05-20"),
      registrationDate = LocalDate.parse("2020-01-15"),
      customerStatus = "ACTIVE",
      customerSegment = "REGULAR",
      acquisitionChannel = "ONLINE",
      acquisitionCampaign = "CAMPAIGN_001",
      preferredDevice = "MOBILE",
      preferredPaymentMethod = "UPI"
    )

  test("should serialize case-class fields using snake_case") {
    val json = JsonMapper.mapper.writeValueAsString(sampleCustomer)
    val node: JsonNode = JsonMapper.mapper.readTree(json)

    assert(node.has("customer_status"))
    assert(node.has("date_of_birth"))
    assert(!node.has("customerStatus"))
  }

  test("should serialize LocalDate fields as ISO date strings") {
    val json = JsonMapper.mapper.writeValueAsString(sampleCustomer)
    val node: JsonNode = JsonMapper.mapper.readTree(json)

    assert(node.get("date_of_birth").asText() == "1990-05-20")
    assert(node.get("registration_date").asText() == "2020-01-15")
  }

  test("should preserve BigDecimal values as JSON numbers") {
    val value = BigDecimal("1234.56")
    val json = JsonMapper.mapper.writeValueAsString(value)
    val node: JsonNode = JsonMapper.mapper.readTree(json)

    assert(node.isNumber)
    assert(BigDecimal(node.decimalValue()) == value)
  }
}
