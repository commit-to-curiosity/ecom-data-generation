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

## 11. Immediate next steps

1. Review the current Git state and confirm the exact revision containing the completed Milestone 1 implementation.
2. Document and agree on the Phase 2 output contract before implementing persistence.
3. Implement the sequential end-to-end generation and writing pipeline.
4. Add automated tests for serialization, output structure, validation failures, and writing failures.
5. Run the full SBT verification suite and record the exact Git revision tested.
6. Update project state and architecture documentation to reflect the verified implementation.
7. Commit the documentation and implementation changes in appropriately scoped commits.
8. Revisit the known `asOfDate` lifecycle-boundary behavior.
9. Implement deterministic parallel generation in accordance with ADR-0003 when its prerequisites and scope are addressed.

Do not treat a planned capability as implemented until source inspection, automated tests, and end-to-end verification support that claim.

## 12. Phase 2: End-to-End Generation and Writing

**Status: Planned**

### 12.1 Objective

Extend the verified Milestone 1 single-customer graph-generation path into an end-to-end pipeline that persists validated generated data to the filesystem and reports the outcome of the run.

The initial implementation will remain sequential. It will preserve the existing entity identifier contract, seeded random-generation behavior, configuration loading, reference-data validation, graph generation, and graph validation.

### 12.2 Planned execution flow

1. Load application and customer configuration.
2. Initialize the seeded random generator.
3. Load and validate geography reference data.
4. Load product reference data.
5. Generate the customer and associated entity graph.
6. Validate the generated graph.
7. Serialize the validated data using the agreed output format.
8. Write output files to the configured output directory.
9. Report output locations, entity counts, and execution status.

### 12.3 Planned deliverables

- **Output contract:** Define the serialization format, file organization, naming conventions, and treatment of existing output files.
- **Dedicated writer:** Separate filesystem operations and serialization from domain generators and `Main`.
- **Pipeline integration:** Connect generation, validation, writing, and execution reporting.
- **Output configuration:** Use a configurable output directory rather than scattering hardcoded filesystem paths throughout the implementation.
- **Failure handling:** Make generation, validation, serialization, and filesystem failures visible. A failed run must not be reported as successful.
- **Automated tests:** Cover successful writes, serialized content, output structure, relevant failure paths, and integration behavior.
- **Verification:** Run the full SBT test suite and the application end to end, then record the exact Git revision and observed results.

The specific output format and atomicity guarantees must be decided before implementation. Writing should not be described as transactional or atomic unless that behavior is implemented and tested.

### 12.4 Acceptance criteria

Phase 2 is complete when:

1. The application generates the expected graph for its configured inputs.
2. Graph validation succeeds before the validated graph is written.
3. The agreed output files are created in the configured directory.
4. Serialized records preserve the required entity fields, identifiers, and relationships.
5. The execution summary reports meaningful record counts and output locations.
6. Invalid data and output failures are handled without falsely reporting success.
7. Automated tests verify both successful and unsuccessful execution paths.
8. The full SBT verification suite and end-to-end execution pass.
9. The architecture and project-state documents accurately describe the implementation and its verified limitations.

### 12.5 Explicit boundaries

The initial Phase 2 implementation will prioritize the existing sequential, single-customer graph path.

The following capabilities are not automatically included in this milestone:

- Multi-customer batch generation.
- Parallel or chunk-based generation.
- Database persistence.
- Performance optimization and large-scale benchmarking.
- Additional output formats beyond the agreed initial format.

Parallel generation must follow ADR-0003, including deterministic chunk planning, seed derivation, bounded execution, and reproducible output ordering. The writer design should remain compatible with future chunked output without prematurely implementing parallel execution.

### 12.6 Relationship to Milestone 1

Milestone 1 remains completed and verified as recorded in Section 7. Phase 2 extends that baseline; it does not retroactively change the scope or verification claims of Milestone 1.