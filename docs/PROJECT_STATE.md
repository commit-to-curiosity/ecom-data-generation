# Project State

Last baseline review: 2026-10-14
Status: Initial documentation baseline; some details require further verification.

## 1. Project identity
- Name: ShopSphere E-Commerce Data Generator
- Scala: 2.13.18
- Organization: `com.shopsphere`
- Application name: `ecom-data-generation`
- Current development branch: `main` (SBT)
- Separate build branch: `main-gradle`

## 2. Repository baseline
At the baseline review:
- `main` was clean and synchronized with `origin/main`.
- HEAD: `7e5cb87` — `test for Customer generation`.
- No tracked Markdown, AsciiDoc, or reStructuredText documentation was found.
- Local Java version: OpenJDK 17.0.16 (Microsoft build).
- Project SBT version: 2.0.9; SBT runner: 1.13.0.
- `build.sbt` declares Scala 2.13.18 and the project dependencies.
- `build.gradle`, `settings.gradle`, `gradle.properties`, and the Gradle wrapper were not present in the `main` checkout at review time.

These are point-in-time observations, not guarantees about later repository state.

## 3. Build and branch model
- `main` contains the SBT build.
- `main-gradle` contains the Gradle migration.
- A previous branch comparison found no differences under `src/` or `conf/` between the branch tips inspected.
- Gradle-specific changes should not be merged blindly into `main`.
- Recheck branch tips and source/test parity before making future alignment claims.

Build configuration observed on `main`:
- Typesafe Config 1.4.3
- Jackson Scala module 2.20.0
- Jackson YAML 2.20.0
- Datafaker 2.5.2
- Log4j API/Core 2.26.1
- ScalaTest 3.2.19

## 4. Architecture inventory
The implementation includes these areas:

- `common.config`: configuration loaders and HOCON, JSON, and YAML readers.
- `common.distribution`: distribution abstractions, uniform/weighted/triangular distributions, and seeded random generation.
- `customer`: customer models, configuration, identity/ID generation, and graph generation.
- `geography`: reference data models, loading, and validation.
- `address`: address model and generator.
- `product`: product models, loading, and reference data.
- `order`: order model and generator.
- `orderitem`: order-item model, ID generation, and generator.
- `payment`: payment model, ID generation, generator, and payment-attempt outcome generation.
- `Main.scala`: application entry point.

Relevant data resources include geography reference JSON, product reference JSON, and configuration examples in `conf/hocon`, `conf/json`, and `conf/yaml`.

## 5. Important integration distinction
A `CustomerGraphGenerator` exists and coordinates customer-related data such as addresses, orders, order items, and payments.

The inspected `Main.scala` path was wired to load customer configuration and generate a customer; it did not yet invoke the full graph generator.

Therefore, distinguish these states in future reviews:
1. Component exists.
2. Component has unit-test coverage.
3. Component is wired into `Main`.
4. End-to-end output has been verified.

Do not treat these as equivalent.

## 6. Reproducibility
`RandomGenerator` wraps `java.util.Random` initialized with a `Long` seed. The inspected entry point used seed `42L`.

A fixed seed can support reproducible sequential execution when random-number consumption and execution order remain unchanged. It does not, by itself, guarantee identical output across parallel worker counts or scheduling patterns.

Any parallel-generation design must explicitly define deterministic partitioning, random stream allocation, ordering, and output guarantees before implementation.

## 7. Current known gaps and review items
- Identifier formats in the implementation conflict with a previously recorded intended convention. See ADR-0001; do not silently normalize either side.
- Confirm the exact order-ID composition and sequence allocation semantics.
- `OrderItemGenerator` contains a local sequence field that appears unused while an ID generator is injected. Verify whether it can be removed safely.
- `Main.scala` does not yet wire the full customer graph into the application execution path, based on the inspected version.
- Define behavior when a configured `asOfDate` precedes a customer's registration date or an order's date.
- Establish explicit requirements for deterministic parallel generation before designing concurrency.
- Build and test commands must be executed and results recorded before claiming the current branch passes verification.

## 8. Verification record
This baseline review inspected Git status, branches, recent history, tracked documentation, and SBT configuration.

It confirmed Java and SBT version commands, but did not run a fresh compilation or test suite as part of this baseline capture.

Previously reported test results must not be treated as verification of the current checkout without rerunning the relevant command.

## 9. Immediate next steps
1. Confirm the canonical entity ID formats.
2. Verify the SBT build and test suite on `main`.
3. Record the current data model, generation flow, and configuration contracts.
4. Document the Gradle verification procedure separately.
5. Decide and document the reproducibility contract before implementing parallel generation.
6. Update this file after each material implementation or verification milestone.
