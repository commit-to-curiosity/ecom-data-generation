# Project State

Last updated: 2026-10-24
Status: Milestone 1 and Phase 2 sequential JSONL export completed and verified. Application-level integration, failure-path coverage, and lifecycle history validation are implemented and tested.

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
- `common.json`: shared Jackson JSON mapper with snake_case field naming and ISO date serialization.
- `common.output`: JSON Lines writer and run output publisher, including record counts and cleanup of temporary output after failures.
- Address export: enriched address records containing building, society, road, area, city, state, and country information.

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

## 6. Integration status distinction

The project contains customer generation, reference-data loading, customer graph generation, and graph validation components.

Milestone 1 has now connected these components through `Main.scala`. The application generates one customer graph, validates its relationships and generation invariants, and logs a concise summary.

Distinguish these states in future reviews:
1. Component exists.
2. Component has unit-test coverage.
3. Component is wired into `Main`.
4. End-to-end execution has been verified.

The current Milestone 1 verification establishes all four states for the single-customer graph-generation path. It does not establish persistence, export, multi-customer batch execution, or parallel execution.

## 7. Milestone 1: Single-Customer Graph Integration

**Status: Completed and verified on 2026-10-22**

### Scope delivered

`Main.scala` now generates one complete `CustomerGraph` sequentially using:

- Random seed: `42L`
- Customers: 1
- Addresses per customer: 2
- Orders per customer: 2
- Order items per order: 2
- As-of date: `customerConfig.lifecycle.asOfDate`

The application loads customer configuration, generates the customer, loads and validates geography reference data, loads product reference data, generates the graph, validates the graph, and logs a concise summary. The graph remains in memory.

### Execution flow

1. Load customer configuration.
2. Initialize the seeded random generator and generate one customer.
3. Load and validate geography reference data.
4. Load product reference data.
5. Generate the customer graph using the configured lifecycle as-of date.
6. Validate graph relationships and generation invariants.
7. Log validation success and graph summary counts.

### Verification record

Commands executed successfully:

- `sbt compile` — compilation passed.
- `sbt "Test / testFull"` — 57 tests passed across 20 suites; 0 failures.
- `sbt run` — application completed successfully.

Observed application output:

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

The payment count is determined by payment-attempt outcomes and is not a fixed generation parameter.

These results verify the tested working tree at the time of execution. Record the exact Git revision separately before making commit-specific verification claims.

### Explicit exclusions

- Parallel or chunk-based execution.
- Database storage, file persistence, or export.
- Multi-customer batch generation.
- Unrelated refactoring.

## 8. Reproducibility
`RandomGenerator` wraps `java.util.Random` initialized with a `Long` seed. The inspected entry point used seed `42L`.

A fixed seed can support reproducible sequential execution when random-number consumption and execution order remain unchanged. It does not, by itself, guarantee identical output across parallel worker counts or scheduling patterns.

Any parallel-generation design must explicitly define deterministic partitioning, random stream allocation, ordering, and output guarantees before implementation.

## 9. Current known gaps and review items

- Define and test behavior when a configured `asOfDate` precedes a customer's registration date or another relevant lifecycle boundary.
- Establish explicit requirements for deterministic parallel generation before implementing concurrency. ADR-0003 defines the design contract; implementation and verification remain pending.
- Expand payment-ID tests if the model later supports multiple payments per order.
- Verify the Gradle migration branch separately; successful SBT verification on `main` does not verify `main-gradle`.
- Record future build and test results against the exact commit tested.
- Persistence, file export, and multi-customer batch generation remain outside Milestone 1.

## 10. Historical and current verification records
Verification performed on `main` after the identifier implementation changes:
- Command: `sbt test`
- Result: **14 tests succeeded, 0 failed**, across 7 suites.
- Scala test compilation completed successfully using Scala 2.13.18.
- `git diff --check` reported no whitespace errors.
- A source/test search for the listed legacy ID patterns returned no matches.
- SBT printed Windows symbolic-link creation warnings recommending Developer Mode; these warnings did not prevent the test suite from passing.

The test result applies to the implementation working tree that was subsequently committed as `bd19e78`. Rerun the relevant commands after later source changes before claiming the new checkout passes verification.

### Milestone 1 verification — 2026-10-22

- `sbt compile`: passed.
- `sbt "Test / testFull"`: 57 tests passed across 20 suites; 0 failures.
- `sbt run`: passed.
- The application logged `Customer graph validation passed`.
- Observed graph counts: 1 customer, 2 addresses, 2 orders, 4 order items, and 3 payments.
- Product reference data loaded: 10 products.
- Windows symbolic-link warnings were present; they did not prevent successful execution.

The Git revision was not included in the supplied command output. Do not attribute these results to a particular commit until the revision is checked.

### Phase 2 verification — 2026-10-24

- `sbt compile`: passed after the address export changes.
- `sbt "Test / testFull"`: 67 tests passed across 24 suites; 0 failures.
- `sbt run`: completed successfully.
- `sbt "run data/exports"`: completed successfully with a custom output root.
- Generated output contained five JSONL files: customers, addresses, orders, order items, and payments.
- Inspected address records included building, society, road, area, city, state, and country fields.
- An integration-style test verified the five output files, JSON object validity, record counts, and address enrichment.
- Windows symbolic-link warnings were present but did not prevent successful execution.
- Exact Git revision: not recorded in the supplied verification output.

## 11. Immediate next steps

1. Review output configuration and inaccessible output paths, including invalid command-line arguments.
2. Continue architecture documentation and review.
3. Revisit lifecycle boundary behavior beyond negative registration history validation.
4. Implement deterministic parallel generation in accordance with ADR-0003 when its prerequisites and scope are addressed.
5. Verify the `main-gradle` branch independently.
6. Record exact Git revisions and test results for future verification.

## 12. Phase 2: End-to-End Generation and Writing

**Status: Core sequential JSONL export implemented and verified on 2026-10-24. Additional application-level integration and failure-path coverage remain pending.**

### 12.1 Objective

Extend the verified Milestone 1 single-customer graph-generation path into a sequential pipeline that validates generated data, serializes the graph into JSON Lines files, publishes completed run output, and reports record counts and output location.

The implementation preserves the existing entity identifier contract, customer configuration loading, seeded random generation, reference-data validation, graph generation, and graph validation.

### 12.2 Implemented execution flow

1. Load customer configuration using the existing configuration reader.
2. Initialize the seeded random generator.
3. Generate one customer and load geography and product reference data.
4. Validate geography reference data and generate the customer graph.
5. Validate graph relationships and generation invariants.
6. Enrich address export records with geographic names resolved from the geography reference data.
7. Serialize records using the shared Jackson JSON mapper.
8. Write five JSON Lines files into a temporary run directory.
9. Publish the completed run directory under the configured output root.
10. Log the published output location and record counts.

### 12.3 Output contract

The output format is JSON Lines (JSONL), with one JSON object per line.

Each run contains these files:

| File | Contents |
|---|---|
| `customers.jsonl` | Customer records |
| `addresses.jsonl` | Address records enriched with geographic names |
| `orders.jsonl` | Order records |
| `order_items.jsonl` | Order-item records |
| `payments.jsonl` | Payment records |

Serialization conventions:

- JSON property names use `snake_case`.
- Date fields are serialized as ISO date strings.
- `BigDecimal` values are represented as JSON numbers.
- Entity identifiers and explicit relationship fields are preserved.
- Address exports include building, society, road, area, city, state, and country names in addition to the address identifiers and postal information.

### 12.4 Output configuration and publication

The default output root is `data/raw`.

The application also accepts a custom output root as its first command-line argument. For example:

- `sbt run` uses `data/raw`.
- `sbt "run data/exports"` uses `data/exports`.

Each run is published under a run-specific directory named `run_<runId>`.

The `JsonLinesWriter` creates parent directories when necessary, writes UTF-8 JSONL records, returns the record count, and refuses to overwrite an existing file.

The `RunOutputPublisher` stages output in a temporary directory and moves the completed directory to its final run-specific location after the writer callback succeeds. If writing fails, it attempts to clean up the temporary directory and propagates the failure. It also refuses to overwrite an existing run directory.

These guarantees are based on the current implementation and tests. The publisher should not be described as providing a filesystem transaction or a guaranteed atomic move unless those guarantees are explicitly implemented and verified.

### 12.5 Automated tests and verification

The following verification was completed on `main` on 2026-10-24:

- `sbt compile`: passed.
- `sbt "Test / testFull"`: **71 tests passed across 25 suites; 0 failures**.
- `sbt run`: completed successfully.
- `sbt "run data/exports"`: completed successfully and published output to a custom directory.
- JSONL output inspection confirmed that all five expected files were present in a generated run directory.
- Address output inspection confirmed that records contained building, society, road, area, city, state, and country fields.
- JSON syntax inspection confirmed that generated JSONL records could be parsed successfully.
- Windows symbolic-link warnings occurred during SBT execution but did not prevent successful verification.

Automated test coverage includes:

- JSON serialization, snake_case property names, ISO date serialization, and numeric `BigDecimal` output.
- JSONL record writing, record counts, empty collections, and overwrite protection.
- Successful run publication, cleanup following simulated write failures, and protection against existing run directories.
- Serialization-failure handling and cleanup of temporary output.
- Invalid output-root handling when the specified root is a file.
- Application-level integration testing through `Main`, covering generation, validation, serialization, publication, all five JSONL files, record counts, and enriched address fields.
- Lifecycle configuration validation that rejects negative `registration_history_days`.

The verified Git revision is `7ea61dcecf3a03c8082f3129c9ffff5d0eb34e39` (`Harden lifecycle validation and output publishing`).

The publisher's tested behavior should not be interpreted as a guarantee of filesystem transactionality or atomic publication in every environment. Inaccessible-path behavior and broader command-line argument validation remain review items.

### 12.6 Acceptance criteria and remaining work

The core sequential JSONL export path and its integration and failure-path coverage are implemented and verified.

Completed acceptance criteria:

1. Generate and validate a complete customer graph.
2. Serialize the graph into the five expected JSONL files.
3. Publish completed output under a run-specific directory.
4. Verify JSON structure, record counts, and enriched geographic address fields.
5. Test the application-level generation-to-publication flow.
6. Test serialization-failure cleanup and invalid output-root behavior.
7. Reject negative registration history configuration values.
8. Run the complete SBT test suite successfully and record the verified Git revision.

Remaining work:

1. Review behavior for inaccessible output paths and invalid command-line arguments.
2. Continue architecture documentation and review.
3. Revisit lifecycle boundary behavior beyond negative registration history validation.
4. Implement deterministic parallel generation in accordance with ADR-0003 only after its prerequisites and scope are addressed.
5. Verify the `main-gradle` branch independently.
6. Record exact Git revisions and test results for future verification.

### 12.7 Explicit boundaries

The initial Phase 2 implementation remains sequential and single-customer.

The following capabilities are not included in the verified Phase 2 scope:

- Multi-customer batch generation.
- Parallel or chunk-based generation.
- Database persistence.
- Performance optimization and large-scale benchmarking.
- Additional output formats beyond JSONL.

Parallel generation must follow ADR-0003, including deterministic chunk planning, seed derivation, bounded execution, and reproducible output ordering.

Milestone 1 remains completed and verified as recorded in Section 7. Phase 2 extends that baseline without retroactively changing Milestone 1's scope or verification claims.