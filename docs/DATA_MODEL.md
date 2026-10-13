# Data Model

## 1. Purpose and scope

This document describes the Scala domain models used by ShopSphere E-Commerce Data Generator and the relationships represented by their fields.

The schemas below reflect the inspected source code. Relationship descriptions and generation behavior are distinguished from constraints explicitly enforced by the model definitions.

## 2. Entity relationship overview

The generated customer graph contains one customer and collections of addresses, orders, order items, and payments.

```text
Customer
  |
  +-- Address
  |     customerId -> Customer.id
  |
  +-- Order
        customerId -> Customer.id
        |
        +-- OrderItem
        |     orderId   -> Order.id
        |     productId -> Product.id (reference)
        |
        +-- Payment
              orderId -> Order.id
```

`CustomerGraph` stores these collections as separate sequences. It does not represent order items or payments as nested fields inside each `Order`.

The arrows describe the intended relationships represented by the identifier fields. The case classes themselves do not declare database foreign keys or enforce referential integrity.

## 3. Entity schemas

### 3.1 Customer

Source: `customer/model/Customer.scala`

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Customer identifier |
| `firstName` | `String` | First name |
| `lastName` | `String` | Last name |
| `email` | `String` | Email address |
| `phone` | `String` | Phone number |
| `gender` | `String` | Gender value |
| `dateOfBirth` | `java.time.LocalDate` | Date of birth |
| `registrationDate` | `java.time.LocalDate` | Registration date |
| `customerStatus` | `String` | Customer status |
| `customerSegment` | `String` | Customer segment |
| `acquisitionChannel` | `String` | Acquisition channel |
| `acquisitionCampaign` | `String` | Acquisition campaign |
| `preferredDevice` | `String` | Preferred device |
| `preferredPaymentMethod` | `String` | Preferred payment method |

Canonical ID example: `CUST000001`.

The ID uses a caller-supplied ordinal, formatted with a six-digit minimum width.

### 3.2 Address

Source: `address/model/Address.scala`

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Address identifier |
| `customerId` | `String` | Associated customer identifier |
| `buildingId` | `String` | Building reference identifier |
| `unitNumber` | `String` | Unit or flat number |
| `postalCode` | `String` | Postal code |

Canonical ID example: `ADDR000001_CUST000001`.

The address sequence is scoped to the customer. The `buildingId` field represents a building reference; the model does not embed the full geography record.

### 3.3 Order

Source: `order/model/Order.scala`

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Order identifier |
| `customerId` | `String` | Associated customer identifier |
| `orderDate` | `java.time.LocalDate` | Order date |
| `orderStatus` | `String` | Order status |
| `totalAmount` | `scala.math.BigDecimal` | Order total |

Canonical ID example: `ORD000001_CUST000001`.

The order sequence is scoped to the customer.

During graph generation, `totalAmount` is assigned the sum of the generated order items' `totalPrice` values.

### 3.4 OrderItem

Source: `order/model/OrderItem.scala`

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Order-item identifier |
| `orderId` | `String` | Associated order identifier |
| `productId` | `String` | Referenced product identifier |
| `quantity` | `Int` | Item quantity |
| `unitPrice` | `scala.math.BigDecimal` | Unit price |
| `totalPrice` | `scala.math.BigDecimal` | Total price for the item |

Canonical ID example: `ITEM000001_ORD000001_CUST000001`.

The order-item sequence is scoped to the order. `productId` identifies a product from product reference data; the `OrderItem` model does not embed the complete product record.

The case class does not itself enforce a formula relating `quantity`, `unitPrice`, and `totalPrice`.

### 3.5 Payment

Source: `payment/model/Payment.scala`

| Field | Scala type | Description |
|---|---|---|
| `id` | `String` | Payment identifier |
| `orderId` | `String` | Associated order identifier |
| `paymentDate` | `java.time.LocalDate` | Payment date |
| `paymentMethod` | `String` | Payment method |
| `paymentStatus` | `String` | Payment status |
| `amount` | `scala.math.BigDecimal` | Payment amount |

Canonical ID example: `PAY000001_ORD000001_CUST000001`.

The payment sequence is scoped to the order. The model does not itself require the payment amount to equal the order total.

### 3.6 CustomerGraph

Source: `customer/model/CustomerGraph.scala`

| Field | Scala type | Description |
|---|---|---|
| `customer` | `Customer` | Customer represented by the graph |
| `addresses` | `Seq[Address]` | Generated addresses |
| `orders` | `Seq[Order]` | Generated orders |
| `orderItems` | `Seq[OrderItem]` | Generated order items |
| `payments` | `Seq[Payment]` | Generated payments |

`CustomerGraph` is a container for the related records. It does not define additional IDs, nested order aggregates, persistence behavior, or model-level validation.

## 4. Relationships and cardinalities

The relationships below are based on the identifier fields and the inspected `CustomerGraphGenerator` implementation.

| Relationship | Represented by | Generation behavior |
|---|---|---|
| Customer to addresses | `Address.customerId` | Generates `addressCount` addresses for the customer |
| Customer to orders | `Order.customerId` | Generates `orderCount` orders for the customer |
| Order to order items | `OrderItem.orderId` | Generates `orderItemCount` items for each order |
| Order to payments | `Payment.orderId` | Generates payments from the payment-attempt outcome |
| Order item to product | `OrderItem.productId` | References product data used during item generation |

For a graph generated with non-negative counts, the requested address count is the number of addresses generated, the requested order count is the number of orders generated, and the requested order-item count applies separately to each order.

Therefore, when the requested counts are `A` addresses, `O` orders, and `I` items per order:

- Address count: `A`
- Order count: `O`
- Order-item count: `O × I`
- Payment count: determined by the generated payment-attempt outcomes

These are generation rules, not database-enforced cardinality constraints. The model definitions alone do not guarantee that every reference resolves or that every order has a payment.

## 5. Identifier conventions

The accepted identifier contract is documented in [ADR-0001: Identifier Conventions](decisions/ADR-0001-identifier-conventions.md).

| Entity | Format | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Caller-supplied ordinal |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order | `ORD000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

Rules:

- Do not put an underscore between an entity prefix and its sequence.
- Use underscores to separate hierarchical identifier components.
- Embed the relevant parent ID in child IDs.
- Keep explicit relationship fields such as `customerId` and `orderId`.
- Reset parent-scoped sequences when generating records for a different parent.

The examples use six-digit sequences. The formatting establishes a minimum width; it does not, by itself, establish a maximum supported sequence value.

## 6. Graph generation behavior

Source: `customer/generator/CustomerGraphGenerator.scala`

The generator accepts:

- `addressCount`
- `orderCount`
- `orderItemCount`

Its observed behavior is:

1. Generate addresses for the supplied customer using geography reference data.
2. Generate orders for the supplied customer.
3. Generate order items for each order using product reference data.
4. Sum the order items' `totalPrice` values and copy that sum into the order's `totalAmount`.
5. Generate a payment-attempt outcome for each order.
6. Generate payments for the statuses returned by that outcome.
7. Collect the records into a `CustomerGraph`.

The payment count is not supplied as a separate method argument. It depends on the generated payment-attempt outcome.

The graph generator receives an as-of date through its primary constructor. Its secondary constructor defaults this date to `2026-01-01`.

## 7. Model constraints and limitations

The inspected case classes are data structures. They do not declare:

- Database primary-key or foreign-key constraints.
- Required non-empty strings or email/phone validation.
- Positive quantity or non-negative monetary-value constraints.
- A mandatory payment-to-order-total equality.
- Automatic referential-integrity validation across the graph.

Such rules, where needed, must be implemented and tested in generation logic or a separate validation layer.

`CustomerGraphGenerator` is implemented separately from the current `Main` execution path. This model documentation does not imply that `Main` currently generates or exports a complete customer graph.

## 8. Related documentation

- [Architecture](ARCHITECTURE.md)
- [Project state](PROJECT_STATE.md)
- [Documentation index](README.md)
- [Identifier conventions (ADR-0001)](decisions/ADR-0001-identifier-conventions.md)

## 9. Maintenance rule

Update this document when a model field, Scala type, relationship, identifier rule, or graph-generation behavior changes. Verify each change against the source and relevant tests.
