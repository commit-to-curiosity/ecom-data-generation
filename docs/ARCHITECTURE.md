# Architecture

## 1. Purpose and scope

ShopSphere E-Commerce Data Generator is a Scala 2.13.18 application for generating e-commerce domain data.

This document describes the architecture observed in the source code. It distinguishes implemented components from components wired into the application entry point. The single-customer graph-generation path has been integrated and verified. Persistence, export, multi-customer batch generation, and parallel execution remain outside the scope of Milestone 1.

## 2. Architectural layers

### 2.1 Configuration

Package: `com.shopsphere.datagen.common.config`

Responsibilities include:
- Reading application configuration.
- Providing format-specific readers for HOCON, JSON, and YAML.
- Creating readers through `ConfigReaderFactory`.
- Loading typed configuration through `ConfigLoader[T]` implementations.

Customer-specific configuration is defined under `customer.config`. Loader implementations reside under `customer.loader`, with `CustomerLoaderFactory` creating the customer configuration loader.

Observed entry-point behavior:

1. `Main` selects `ConfigFileFormat.HOCON`.
2. `ConfigReaderFactory` creates the corresponding reader.
3. The reader loads application configuration.
4. `CustomerLoaderFactory` creates a customer configuration loader.
5. The loader loads customer configuration from the configured customer section.

The format selection in `Main` is currently hardcoded to HOCON.

### 2.2 Shared generation infrastructure

Package: `com.shopsphere.datagen.common`

Important components:
- `generator.Generator[T]`: common abstraction exposing `generate(): T`.
- `distribution.RandomGenerator`: seeded random-number generation backed by `java.util.Random`.
- `distribution.Distribution[T]`: distribution abstraction.
- `UniformDistribution[T]`, `WeightedDistribution[T]`, and `TriangularDistribution`: distribution implementations.
- `json.JsonMapper`: shared JSON mapping support.
- `enums`: domain values and configuration-format types.

A fixed random seed supports reproducibility only when random-number consumption and execution order remain consistent. It does not establish deterministic results across different parallel scheduling patterns.

### 2.3 Customer generation

Package: `com.shopsphere.datagen.customer`

Important components:
- `CustomerConfig` and related configuration models describe customer-generation inputs.
- `CustomerConfigLoader` and supporting loaders load typed customer configuration.
- `CustomerIdentityGenerator` generates customer identity attributes.
- `CustomerIdGenerator` generates canonical customer IDs.
- `CustomerGenerator` generates a `Customer`.
- `CustomerGraphGenerator` coordinates related entities for a customer.
- `CustomerGraphValidator` validates graph relationships and generation invariants.
- `Customer` and `CustomerGraph` represent the customer and its generated graph.

The customer configuration includes separate concerns such as demographics, lifecycle, status, segmentation, acquisition, and behavior.

### 2.4 Reference data

Geography components are under `com.shopsphere.datagen.geography`:
- `GeographyLoader` loads geography reference data.
- `GeographyModels` and `GeographyJsonModels` represent geography data.
- `GeographyReferenceData` provides reference data to consumers.
- `GeographyValidator` validates geography relationships.
- `BuildingLocation` supports building-location data.

Product components are under `com.shopsphere.datagen.product`:
- `ProductLoader` loads product data.
- `Product` and `ProductsData` represent product data.
- `ProductReferenceData` provides products to generators.

`CustomerGraphGenerator` receives geography and product reference data as constructor dependencies. `Main` loads these resources, validates geography reference data, and passes the data to the graph generator.

### 2.5 Domain generation

| Domain | Main components | Responsibility |
|---|---|---|
| Address | `AddressGenerator`, `Address` | Generate addresses associated with a customer using geography reference data |
| Order | `OrderGenerator`, `Order` | Generate orders associated with a customer |
| Order item | `OrderItemGenerator`, `OrderItem`, `OrderItemIdGenerator` | Generate order lines associated with an order and selected products |
| Payment | `PaymentGenerator`, `Payment`, `PaymentIdGenerator` | Generate payment records associated with an order |
| Payment outcome | `PaymentAttemptOutcomeGenerator` | Select payment-attempt outcomes and their associated payment statuses |

## 3. Current application entry-point flow

Source: `src/main/scala/com/shopsphere/datagen/Main.scala`

The verified `Main.main` path performs these operations:

1. Logs application startup.
2. Selects HOCON configuration.
3. Creates a configuration reader and reads the application configuration.
4. Loads `CustomerConfig`.
5. Creates `RandomGenerator` with seed `42L`.
6. Creates `CustomerIdentityGenerator`.
7. Creates `CustomerGenerator`.
8. Generates one customer.
9. Loads geography reference data and validates its integrity.
10. Loads product reference data and constructs `ProductReferenceData`.
11. Creates `CustomerGraphGenerator` with the generated customer, reference data, seeded random generator, ID generators, and `customerConfig.lifecycle.asOfDate`.
12. Generates two addresses, two orders, and two order items per order.
13. Validates the generated graph using `CustomerGraphValidator`.
14. Logs validation success and a concise graph summary.

The generated graph is retained in memory. The verified execution path does not persist or export it to a database or file.

### 3.1 Verified execution result

The application completed successfully with random seed `42L`.

Observed summary from the verified run:

| Entity or operation | Result |
|---|---:|
| Customer ID | `CUST000001` |
| Addresses | 2 |
| Orders | 2 |
| Order items | 4 |
| Payments | 3 |
| Products loaded | 10 |
| Geography validation | Passed |
| Customer graph validation | Passed |

The number of payments depends on the generated payment-attempt outcome rather than a fixed payment-count parameter.

### 3.2 Verification commands

The following commands completed successfully during Milestone 1 verification:

```powershell
sbt compile
sbt "Test / testFull"
sbt run
```

The full test run completed with 57 tests across 20 suites and zero failures. The application run logged `Customer graph validation passed` and printed the graph summary.

These results apply to the working tree tested at that time. The exact Git revision should be recorded separately before attributing the verification to a specific commit.

## 4. Customer graph generation flow

Source: `customer/generator/CustomerGraphGenerator.scala`

`CustomerGraphGenerator` receives:
- A `Customer`.
- `GeographyReferenceData`.
- `ProductReferenceData`.
- A shared `RandomGenerator`.
- An `OrderItemIdGenerator`.
- A `PaymentIdGenerator`.
- An as-of date.

The graph-generation path in `Main` explicitly supplies `customerConfig.lifecycle.asOfDate`. It does not rely on the secondary constructor that defaults the as-of date to `2026-01-01`.

Its `generate(addressCount, orderCount, orderItemCount)` method follows this sequence:

1. Creates one `AddressGenerator` for the customer and generates the requested number of addresses.
2. Creates one `OrderGenerator` for the customer and generates the requested number of orders.
3. For each generated order, creates an `OrderItemGenerator`.
4. Generates the requested number of order items for that order.
5. Calculates the order total from the generated order-item totals and copies the total into the order.
6. Uses `PaymentAttemptOutcomeGenerator` to obtain payment statuses for the order.
7. Creates a `PaymentGenerator` for that order and generates payments for those statuses.
8. Collects the customer, addresses, orders, order items, and payments into a `CustomerGraph`.

The method accepts counts for addresses, orders, and order items per order. The number of payments is determined by the generated payment-attempt outcome rather than by a separate payment-count parameter.

### 4.1 Graph validation

`CustomerGraphValidator` validates the generated graph against geography and product reference data and the configured as-of date.

The validation path checks the graph's key invariants, including:
- Customer identity and lifecycle date constraints.
- Address relationships and references to known buildings and postal codes.
- Order relationships and date constraints.
- Order-item references, product references, quantities, prices, identifiers, and item counts.
- Consistency between order totals and the sum of order-item totals.
- Payment references, identifiers, amounts, and date constraints.
- Duplicate identifiers within relevant entity collections.

Validation failures raise exceptions rather than allowing `Main` to log a successful graph-validation result.

## 5. Identifier generation and sequence scope

The accepted identifier contract is recorded in `decisions/ADR-0001-identifier-conventions.md`.

| Entity | Example | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Caller-supplied ordinal |
| Order | `ORD000001_CUST000001` | Per customer |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

The order-item and payment ID generators maintain counters keyed by order ID using concurrent maps and atomic counters. This scopes sequences to their parent order.

See ADR-0001 for the full identifier rules.

## 6. Randomness and deterministic parallel generation

`Main` currently constructs `RandomGenerator` with seed `42L`. The graph generator passes a random generator to multiple domain generators. A fixed seed does not by itself guarantee identical results under different parallel scheduling patterns.

[ADR-0003](decisions/ADR-0003-deterministic-parallel-generation.md) records the accepted design for deterministic parallel generation:

- One independent random stream per logical chunk.
- An initial chunk size of 10,000 customers.
- Stable global customer ordinals and deterministic chunk boundaries.
- Chunk-local mutable random state and generator instances.
- Bounded parallel execution.
- Deterministic chunk output ordering.
- Canonical output equality across worker counts as a test requirement.

The design is not yet implemented or verified. The seed-derivation algorithm must be specified and tested during implementation. The guarantee is scoped to fixed inputs, chunk size, seed derivation, generator version, and random draw order. Changes to those inputs or rules may change generated values.

## 7. Architectural status and boundaries

| Capability | Observed status |
|---|---|
| Format-specific configuration readers | Implemented |
| Customer configuration loading | Implemented |
| Single-customer generation in `Main` | Wired and verified |
| Geography reference-data loading and validation | Wired and verified |
| Product reference-data loading | Wired and verified |
| Customer graph orchestration | Implemented and unit-tested |
| Graph generation in `Main` | Wired and verified |
| Graph validation in `Main` | Wired and verified |
| End-to-end single-customer graph generation | Verified by `sbt run` |
| Database or file export from `Main` | Not implemented in the verified execution path |
| Multi-customer batch generation | Outside Milestone 1 |
| Deterministic parallel generation | Defined in ADR-0003; implementation and verification pending |

Milestone 1 verification used `sbt compile`, `sbt "Test / testFull"`, and `sbt run`. The test run completed with 57 tests across 20 suites and zero failures.

The distinction between implementation, unit testing, application wiring, and end-to-end verification must be maintained when assessing project readiness.

## 8. Related documentation

- [Project state](PROJECT_STATE.md)
- [Documentation index](README.md)
- [Decision records](decisions/README.md)
- [Identifier conventions (ADR-0001)](decisions/ADR-0001-identifier-conventions.md)
- [Build-system branch strategy (ADR-0002)](decisions/ADR-0002-build-system-branch-strategy.md)
- [Deterministic parallel generation (ADR-0003)](decisions/ADR-0003-deterministic-parallel-generation.md)

## 9. Maintenance rule

Update this document when component responsibilities, constructor dependencies, generation flow, or application wiring change. Verify descriptions against the current source rather than relying on older summaries.
