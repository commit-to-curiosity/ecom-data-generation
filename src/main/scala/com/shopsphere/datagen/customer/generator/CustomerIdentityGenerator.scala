package com.shopsphere.datagen.customer.generator

import com.shopsphere.datagen.common.distribution.RandomGenerator
import net.datafaker.Faker

class CustomerIdentityGenerator(
                                 randomGenerator: RandomGenerator
                               ) {

  private val faker = new Faker(randomGenerator.randomInstance)

  def generateFirstName(): String = {
    faker.name().firstName()
  }

  def generateLastName(): String = {
    faker.name().lastName()
  }

  def generateEmail(firstName: String, lastName: String): String = {
    faker.internet().emailAddress(s"$firstName.$lastName")
  }

  def generatePhone(): String = {
    faker.phoneNumber().phoneNumber()
  }
}