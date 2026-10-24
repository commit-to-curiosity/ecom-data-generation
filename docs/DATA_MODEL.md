# Data Model

## 1. Purpose and scope

This document describes the Scala domain models used by the ShopSphere E-Commerce Data Generator, the relationships represented by their fields, the entity identifier conventions, and the records published by the generation pipeline.

The schemas in this document reflect the inspected Scala model definitions. Relationship descriptions and generation behavior are distinguished from constraints explicitly enforced by the model definitions and validators.

The customer graph describes domain records. Chunk execution, parallel processing, output writing, and run publication are separate execution-layer responsibilities documented in [Architecture](ARCHITECTURE.md).

## 2. Entity relationship overview

The generated customer graph contains one customer and collections of addresses, orders, order items, and payments.

The intended entity relationships are:

- A customer can have multiple addresses.
- A customer can have multiple orders.
- Each address belongs to a customer.
- Each order belongs to a customer.
- An order can contain multiple order items.
- Each order item belongs to an order and references a product.
- An order can have payment records associated with its payment attempts.

The relationship structure can be represented as:

```text
Customer
   |
   +---- Address
   |       customerId
   |
   +---- Order
           customerId
              |
              +---- OrderItem
              |       orderId
              |       productId
              |           |
              |           +---- Product reference data
              |
              +---- Payment
                      orderId
```

`CustomerGraph` stores addresses, orders, order items, and payments as separate collections. It does not nest order items or payments inside individual `Order` objects. Their relationships are represented through explicit identifier fields.

These are application-level relationships, not database foreign-key constraints. The application validator checks the relationships and invariants implemented in its validation logic.

## 3. Entity identifiers

The canonical identifier convention is defined in [ADR-0001](decisions/ADR-0001-identifier-conventions.md).

| Entity | Example | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Caller-supplied ordinal |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order | `ORD000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

Entity prefixes are directly adjacent to their six-digit, zero-padded sequence. Underscores separate hierarchical identifier components.

The sequence has a minimum width of six digits. The format does not imply that sequence values are capped at `999999`.

Child identifiers embed their parent identifier, but explicit relationship fields remain necessary. Embedded identifiers do not replace the need to validate referential consistency.

Order-item and payment identifiers use order-scoped counters. Identifier uniqueness across a complete generated dataset should be established through appropriate integration tests rather than inferred only from individual generator tests.

## 4. Customer

The `Customer` model represents customer identity and customer attributes used by the generation pipeline.

### 4.1 Scala model

Source: `src/main/scala/com/shopsphere/datagen/customer/model/Customer.scala`

```scala
case class Customer(
  id: String,
  firstName: String,
  lastName: String,
  email: String,
  phone: String,
  gender: String,
  dateOfBirth: LocalDate,
  registrationDate: LocalDate,
  customerStatus: String,
  customerSegment: String,
  acquisitionChannel: String,
  acquisitionCampaign: String,
  preferredDevice: String,
  preferredPaymentMethod: String
)
```

### 4.2 Field definitions

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Canonical customer identifier |
| `firstName` | `String` | Customer first name |
| `lastName` | `String` | Customer last name |
| `email` | `String` | Customer email address |
| `phone` | `String` | Customer phone number |
| `gender` | `String` | Customer gender value |
| `dateOfBirth` | `LocalDate` | Customer date of birth |
| `registrationDate` | `LocalDate` | Customer registration date |
| `customerStatus` | `String` | Customer status |
| `customerSegment` | `String` | Customer segment |
| `acquisitionChannel` | `String` | Customer acquisition channel |
| `acquisitionCampaign` | `String` | Customer acquisition campaign |
| `preferredDevice` | `String` | Preferred customer device |
| `preferredPaymentMethod` | `String` | Preferred payment method |

### 4.3 Generation responsibilities

Customer identity generation and customer ID allocation are separate concerns:

- `CustomerIdentityGenerator` generates identity attributes.
- `CustomerIdGenerator` constructs the canonical customer identifier.
- `CustomerGenerator` creates the customer record using the relevant configuration and generation dependencies.

The customer ordinal is supplied by the generation pipeline. For chunk-based generation, stable global ordinals are important for identifier allocation and reproducibility.

## 5. Address

An address belongs to a customer and uses the canonical address identifier format.

### 5.1 Internal Scala model

Source: `src/main/scala/com/shopsphere/datagen/address/model/Address.scala`

```scala
case class Address(
  id: String,
  customerId: String,
  buildingId: String,
  unitNumber: String,
  postalCode: String
)
```

### 5.2 Field definitions

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Canonical address identifier |
| `customerId` | `String` | Identifier of the owning customer |
| `buildingId` | `String` | Building reference identifier |
| `unitNumber` | `String` | Unit or flat number |
| `postalCode` | `String` | Postal code |

Address generation uses geography reference data. The internal model stores geographic reference identifiers rather than duplicating all geographic names.

Address sequence scope is per customer.

### 5.3 Enriched address export

Source: `src/main/scala/com/shopsphere/datagen/address/model/AddressExport.scala`

The JSONL output uses `AddressExport`, which is an enriched representation of the internal `Address` model.

Its fields are:

| Field | Scala type | Source |
|---|---|---|
| `id` | `String` | Internal address |
| `customerId` | `String` | Internal address |
| `buildingId` | `String` | Internal address |
| `unitNumber` | `String` | Internal address |
| `postalCode` | `String` | Internal address |
| `buildingName` | `String` | Resolved building |
| `societyName` | `String` | Resolved society |
| `roadName` | `String` | Resolved road |
| `areaName` | `String` | Resolved area |
| `cityName` | `String` | Resolved city |
| `stateName` | `String` | Resolved state |
| `countryName` | `String` | Resolved country |

During output mapping, `Main.scala` resolves the address's `buildingId` through `geographyReferenceData.resolveBuilding(...)`.

The resolved building provides access to its associated society, road, area, city, state, and country. If the building cannot be resolved, the mapping throws an `IllegalStateException`.

Consequently, `addresses.jsonl` contains enriched address records rather than direct serialization of the internal `Address` model.

## 6. Order

An order belongs to a customer and uses the canonical order identifier format.

### 6.1 Scala model

Source: `src/main/scala/com/shopsphere/datagen/order/model/Order.scala`

```scala
case class Order(
  id: String,
  customerId: String,
  orderDate: LocalDate,
  orderStatus: String,
  totalAmount: BigDecimal
)
```

### 6.2 Field definitions

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Canonical order identifier |
| `customerId` | `String` | Identifier of the owning customer |
| `orderDate` | `LocalDate` | Order creation date |
| `orderStatus` | `String` | Order status |
| `totalAmount` | `BigDecimal` | Total amount calculated from order items |

### 6.3 Generation behavior

Order sequence scope is per customer.

Order generation consumes the applicable customer and generation settings, including the selected scenario's order behavior.

The number of orders per customer is scenario-dependent. For example, the `baseline` and `high_demand` scenarios can configure different order counts.

The order's relationship to its customer is represented by the explicit `customerId` field as well as by the hierarchical identifier convention.

The generated order total is calculated from its generated order items.

## 7. Order item

An order item represents a line within an order and references the selected product.

### 7.1 Scala model

Source: `src/main/scala/com/shopsphere/datagen/order/model/OrderItem.scala`

```scala
case class OrderItem(
  id: String,
  orderId: String,
  productId: String,
  quantity: Int,
  unitPrice: BigDecimal,
  totalPrice: BigDecimal
)
```

### 7.2 Field definitions

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Canonical order-item identifier |
| `orderId` | `String` | Identifier of the owning order |
| `productId` | `String` | Product reference identifier |
| `quantity` | `Int` | Number of product units |
| `unitPrice` | `BigDecimal` | Price per unit |
| `totalPrice` | `BigDecimal` | Total line price |

### 7.3 Generation behavior

The canonical order-item identifier embeds the order identifier:

```text
ITEM000001_ORD000001_CUST000001
```

Order-item sequence scope is per order.

Product selection behavior can vary by scenario, including uniform selection and weighted selection.

The generator calculates the line total using:

\[
\text{totalPrice} = \text{quantity} \times \text{unitPrice}
\]

If an order contains \(I\) generated items and a customer has \(O\) generated orders, the number of order items for that customer is:

\[
\text{Order-item count} = O \times I
\]

This formula assumes the configured item count is applied to every generated order. If generation behavior later allows variable item counts per order, the total must instead be calculated by summing the item counts for the generated orders.

## 8. Payment

Payment records are associated with orders and use the canonical payment identifier format.

### 8.1 Scala model

Source: `src/main/scala/com/shopsphere/datagen/payment/model/Payment.scala`

```scala
case class Payment(
  id: String,
  orderId: String,
  paymentDate: LocalDate,
  paymentMethod: String,
  paymentStatus: String,
  amount: BigDecimal
)
```

### 8.2 Field definitions

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Canonical payment identifier |
| `orderId` | `String` | Identifier of the associated order |
| `paymentDate` | `LocalDate` | Payment date |
| `paymentMethod` | `String` | Payment method |
| `paymentStatus` | `String` | Payment status |
| `amount` | `BigDecimal` | Payment amount |

### 8.3 Generation behavior

The canonical payment identifier embeds the order identifier:

```text
PAY000001_ORD000001_CUST000001
```

Payment sequence scope is per order.

Payment generation is affected by payment-attempt outcome behavior:

- One payment-attempt outcome is generated for each order.
- The outcome determines the payment statuses generated for that order.
- An outcome can therefore result in multiple payment records associated with the same order.

The number of payment records is not necessarily equal to the number of orders or a fixed configured count. It depends on the actual payment-attempt generation rules and outcomes.

The payment model, outcome generator, and scenario configuration should be considered together when interpreting payment counts or failure rates.

## 9. Customer graph

`CustomerGraph` represents a customer and the related generated entities.

### 9.1 Scala model

Source: `src/main/scala/com/shopsphere/datagen/customer/model/CustomerGraph.scala`

```scala
case class CustomerGraph(
  customer: Customer,
  addresses: Seq[Address],
  orders: Seq[Order],
  orderItems: Seq[OrderItem],
  payments: Seq[Payment]
)
```

### 9.2 Graph generation

`CustomerGraphGenerator` coordinates the generation of addresses, orders, order items, and payments for a customer.

It receives the relevant dependencies, including:

- Customer.
- Geography reference data.
- Product reference data.
- Random-generation infrastructure.
- Order-item and payment identifier generators.
- Applicable as-of date.
- Generation scenario.

The generator accepts three cardinality inputs:

- `addressCount`: number of addresses to generate.
- `orderCount`: number of orders to generate.
- `orderItemCount`: number of order items generated for each order.

These counts must be non-negative.

For each generated order, the generator:

1. Generates the order.
2. Generates the configured number of order items.
3. Calculates the order total from the generated item totals.
4. Generates a payment-attempt outcome.
5. Generates payment records corresponding to the outcome's payment statuses.

The resulting records are assembled into the separate collections of `CustomerGraph`.

### 9.3 Graph validation

`CustomerGraphValidator` checks graph relationships and generation invariants.

Its guarantees are limited to the checks actually implemented by the validator. These checks are described in Section 12.

The graph model describes related domain records. It does not, by itself, describe the full chunk-execution, parallel-processing, serialization, or publication lifecycle.

## 10. Generation cardinalities

The number of generated records depends on run settings and scenario configuration.

| Entity | Cardinality behavior |
|---|---|
| Customers | Controlled by the total-customer run setting |
| Addresses | Controlled by address-generation settings |
| Orders | Controlled by order-generation settings and scenario |
| Order items | Controlled by item-generation settings for each order |
| Payments | Determined by payment-attempt outcomes and payment-generation behavior |

For a customer with \(A\) addresses, \(O\) orders, and \(I\) items per order:

\[
\text{Order-item count} = O \times I
\]

Payment count is determined separately by the payment-attempt outcomes for each order.

### 10.1 Default run configuration

The default `GenerationRunConfig` values are:

| Setting | Default |
|---|---:|
| `masterSeed` | `42` |
| `totalCustomers` | `100` |
| `chunkSize` | `25` |
| `parallelism` | `2` |
| `maxInFlight` | `4` |

These are run-level settings, not properties of the domain model.

The configuration requires:

- A non-negative master seed.
- A non-negative total customer count.
- A positive chunk size.
- Positive parallelism.
- `maxInFlight` greater than or equal to parallelism.

Scenario configuration can change business-generation behavior without changing the fundamental entity relationships.

## 11. Serialization and output

The main generation pipeline publishes five JSONL files:

| File | Record type |
|---|---|
| `customers.jsonl` | Customer |
| `addresses.jsonl` | AddressExport |
| `orders.jsonl` | Order |
| `order_items.jsonl` | OrderItem |
| `payments.jsonl` | Payment |

The serialization contract includes:

- UTF-8 encoding.
- One JSON object per line.
- Snake-case property names.
- ISO-formatted date strings.
- JSON numeric representation for `BigDecimal` values.
- Preservation of entity identifiers and explicit relationship fields.

Address exports additionally include resolved geographic names, as described in Section 5. The export mapping should be treated as the output contract; it should not be assumed that every exported property is a field of the internal domain model.

The pipeline initializes all five output files, including when the configured total customer count is zero. Generated records are then written as chunks are processed.

## 12. Validation boundaries

Validation responsibilities should remain distinct.

### 12.1 Reference-data integrity

As part of customer graph validation, `CustomerGraphValidator` checks
that referenced geographic and product records exist in the relevant
reference data.

These checks include:

- Address building identifiers against geography reference data.
- Address postal codes against geography reference data.
- Order-item product identifiers against product reference data.

These checks establish reference integrity for the generated graph.
They do not imply that all reference-data quality or business rules
are validated.

### 12.2 Structural graph validation

The current `CustomerGraphValidator` checks the following invariants.

**Customer**

- The customer is not null.
- The customer identifier follows the canonical `CUST` identifier format.
- The registration date is not after the applicable as-of date.

**Address**

- Address identifiers are unique within the customer graph.
- Each address belongs to the graph's customer.
- Address identifiers follow the canonical address identifier format.
- Referenced building identifiers exist in geography reference data.
- Referenced postal codes exist in geography reference data.

**Order**

- Order identifiers are unique within the customer graph.
- Each order belongs to the graph's customer.
- Order identifiers follow the canonical order identifier format.
- Order dates are not before customer registration.
- Order dates are not after the applicable as-of date.

**Order item**

- Order-item identifiers are unique within the customer graph.
- Each order item references an order in the graph.
- Order-item identifiers follow the canonical hierarchical identifier format.
- Quantity is positive.
- Unit price is non-negative.
- Total price equals quantity multiplied by unit price.
- Referenced product identifiers exist in product reference data.
- Each order contains the expected number of generated items.
- Each order's total amount equals the sum of its item totals.

**Payment**

- Payment identifiers are unique within the customer graph.
- Each payment references an order in the graph.
- Payment identifiers follow the canonical payment identifier format.
- Payment amount equals the associated order total.
- Payment dates are not before the associated order date.
- Payment dates are not after the applicable as-of date.

Identifier uniqueness checked by this validator is scoped to an individual `CustomerGraph`. It should not be interpreted as proof of uniqueness across the complete generated dataset.

### 12.3 Statistical validation

Statistical validation checks whether distributions, frequencies, correlations, and business metrics behave as intended.

Passing structural validation does not establish statistical realism. Statistical acceptance criteria belong to the realistic-dataset roadmap and must be defined separately.

## 13. Related documentation

- [Documentation index](README.md)
- [Architecture](ARCHITECTURE.md)
- [Project state](PROJECT_STATE.md)
- [Identifier conventions (ADR-0001)](decisions/ADR-0001-identifier-conventions.md)
- [Deterministic parallel generation (ADR-0003)](decisions/ADR-0003-deterministic-parallel-generation.md)

## 14. Maintenance rule

Update this document when model fields, entity relationships, identifier formats, cardinality rules, scenario behavior, validation guarantees, or serialization contracts change.

Use the current Scala model definitions, generators, validators, and output mapping as the source of truth.
