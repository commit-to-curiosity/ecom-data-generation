# Project State

Last updated: 2026-10-14
Status: Canonical hierarchical identifier contract implemented and tested on `main`. Further architecture and integration work remains.

## 1. Project identity
- Name: ShopSphere E-Commerce Data Generator
- Scala: 2.13.18
- Organization: `com.shopsphere`
- Application name: `ecom-data-generation`
- Current development branch: `main` (SBT)
- Separate build branch: `main-gradle`

## 2. Repository baseline
The original repository baseline was captured before the documentation and identifier implementation commits. Its historical HEAD was `7e5cb87` (`test for Customer generation`).

Current repository state at the latest verification:
- Branch: `main`.
- Identifier implementation commit: `bd19e78` (`feat: implement hierarchical entity identifiers`).
- Documentation commits preceding the implementation:
  - `0293773` — `docs: establish project knowledge baseline`
  - `7b11129` — `docs: accept hierarchical identifier contract`
- The implementation and test changes were committed separately from the initial documentation decisions.
- The branch was reported clean after the implementation commit and was three commits ahead of `origin/main` at that point. Recheck Git status before future work; this document update may add another local commit.
- Local Java version: OpenJDK 17.0.16 (Microsoft build).
- SBT version reported by the project: 2.0.9; SBT runner: 1.13.0.
- `build.sbt` declares Scala 2.13.18 and project dependencies.
- The `main-gradle` branch is the separate Gradle migration branch. Recheck its current state before making branch-parity claims.

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

## 5. Canonical entity identifier contract
Decision: `docs/decisions/ADR-0001-identifier-conventions.md` — Accepted.

The implementation uses the following formats:

| Entity | Format | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Caller-supplied global ordinal |
| Order | `ORD000001_CUST000001` | Per customer |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

Rules:
- Do not put an underscore between an entity prefix and its zero-padded six-digit sequence.
- Use underscores to separate hierarchical ID components.
- Child identifiers embed their relevant parent identifier.
- Explicit relationship fields, such as `customerId` and `orderId`, remain required; embedded IDs do not replace these fields.
- Order and address counters reset for each customer through generator-instance scope.
- Order-item and payment counters are keyed by order ID, allowing each order's sequence to start at one.
- Order-item and payment ID generators use concurrent maps and atomic counters to support concurrent calls without sharing a single global sequence across all parents.

The source and test scan performed after implementation found no remaining matches for the searched legacy patterns `CUST_`, `ORD_CUST_`, `ADDR_`, `ITEM_`, or `PAY_` under `src/main/scala` and `src/test/scala`. This scan does not establish that no legacy identifiers exist in external datasets, generated files, or other directories.

## 6. Important integration distinction
A `CustomerGraphGenerator` exists and coordinates customer-related data such as addresses, orders, order items, and payments.

The inspected `Main.scala` path was wired to load customer configuration and generate a customer; it did not yet invoke the full graph generator.

Therefore, distinguish these states in future reviews:
1. Component exists.
2. Component has unit-test coverage.
3. Component is wired into `Main`.
4. End-to-end output has been verified.

Do not treat these as equivalent. The identifier implementation commit does not, by itself, establish that full-graph generation is wired into the application entry point.

## 7. Reproducibility
`RandomGenerator` wraps `java.util.Random` initialized with a `Long` seed. The inspected entry point used seed `42L`.

A fixed seed can support reproducible sequential execution when random-number consumption and execution order remain unchanged. It does not, by itself, guarantee identical output across parallel worker counts or scheduling patterns.

Any parallel-generation design must explicitly define deterministic partitioning, random stream allocation, ordering, and output guarantees before implementation.

## 8. Current known gaps and review items
- `Main.scala` does not yet wire the full customer graph into the application execution path, based on the inspected version.
- Define behavior when a configured `asOfDate` precedes a customer's registration date or an order's date.
- Establish explicit requirements for deterministic parallel generation before designing concurrency.
- Expand payment-ID tests if the model later supports multiple payments per order; current graph assertions verify the first payment ID for each order, and the existing payment generator tests verify payment ID generation.
- Verify the Gradle migration branch separately; the successful SBT test run on `main` does not verify `main-gradle`.
- Record future build/test results against the exact commit tested.

## 9. Verification record
Verification performed on `main` after the identifier implementation changes:
- Command: `sbt test`
- Result: **14 tests succeeded, 0 failed**, across 7 suites.
- Scala test compilation completed successfully using Scala 2.13.18.
- `git diff --check` reported no whitespace errors.
- A source/test search for the listed legacy ID patterns returned no matches.
- SBT printed Windows symbolic-link creation warnings recommending Developer Mode; these warnings did not prevent the test suite from passing.

The test result applies to the implementation working tree that was subsequently committed as `bd19e78`. Rerun the relevant commands after later source changes before claiming the new checkout passes verification.

## 10. Immediate next steps
1. Commit this project-state update.
2. Push the local `main` commits when ready.
3. Record the current data model, generation flow, and configuration contracts.
4. Document the Gradle verification procedure separately.
5. Decide and document the reproducibility contract before implementing parallel generation.
6. Revisit the `asOfDate` boundary behavior and full-graph application wiring.