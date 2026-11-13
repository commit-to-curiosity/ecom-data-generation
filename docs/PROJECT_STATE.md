# Project State

**Last updated:** 2026-11-02

## Verification and evidence policy

This document summarizes the known implementation status of ShopSphere E-Commerce Data Generator and the next planned work.

Git state, test results, and implementation claims are point-in-time evidence. Recheck the repository and rerun relevant tests before making new verification claims.

A test result must not be attributed to a commit unless the exact tested revision is known. Historical test records are retained for traceability and must not be interpreted as verification of the current working tree.

Implementation status, structural correctness, statistical realism, performance, and production readiness are separate claims. Each requires its own evidence.

## 1. Project identity

- **Project name:** ShopSphere E-Commerce Data Generator
- **Application name:** `ecom-data-generation`
- **Organization:** `com.shopsphere`
- **Scala version:** 2.13.18
- **Java version used for development:** OpenJDK 17
- **Primary development branch:** `main` (SBT)
- **Separate build migration branch:** `main-gradle`

The purpose of the project is to generate configurable, relationally consistent e-commerce datasets suitable for data engineering development, experimentation, and downstream processing.

## 2. Repository and build model

### Branch model

- `main` is the primary SBT development branch.
- `main-gradle` is the separate Gradle build migration branch.
- Do not assume that the two branches are identical.
- Verify source, configuration, tests, and build behavior independently before making branch-parity claims.

Git commit IDs and ahead/behind counts are intentionally not maintained here because they change with every commit. Use `git status -sb` and `git log -5 --oneline --decorate` for the current repository snapshot.

### Build model

The primary development build uses SBT with Scala 2.13.18 and Java 17.

The SBT build has previously used the following dependencies:

- Typesafe Config 1.4.3
- Jackson Scala module 2.20.0
- Jackson YAML 2.20.0
- Datafaker 2.5.2
- Log4j API/Core 2.26.1
- ScalaTest 3.2.19

Recheck the current build files before treating this as a complete or current dependency inventory.

The Gradle migration is maintained separately. Successful builds or tests on one branch do not establish equivalent behavior on the other branch.

## 3. Architecture inventory

The implementation includes the following areas:

- `common.config`: configuration loaders and HOCON, JSON, and YAML readers.
- `common.distribution`: distribution abstractions, uniform and weighted distributions, triangular distributions, and seeded random generation.
- `common.json`: shared Jackson JSON serialization configuration, including snake_case property naming and ISO date serialization.
- `common.output`: JSON Lines writing and run output publication.
- `common.parallel`: chunk planning and bounded parallel execution.
- `customer`: customer models, configuration, identifier generation, and customer graph generation.
- `geography`: geographic reference data, loading, and validation.
- `address`: address models and generation.
- `product`: product models, reference data, and product selection.
- `order`: order models and generation.
- `orderitem`: order-item models, identifier generation, and generation.
- `payment`: payment models, identifier generation, and payment-attempt outcome generation.
- `scenario`: generation scenario definitions and configuration loading.
- `Main.scala`: application entry point and generation pipeline.

The project also contains reference data and configuration resources under `conf`, including scenario configuration under `conf/hocon`.

This inventory is a high-level index, not a guarantee that every class, configuration file, or resource is listed.

For detailed entity structures and relationships, see [Data Model](DATA_MODEL.md).

## 4. Current delivery status

### Milestone 1: Integrated Customer Graph Generation

**Status: Completed**

Customer generation, reference-data loading, customer graph generation, and graph validation were connected through the application entry point.

The milestone established the initial integrated customer graph, including:

- Customer generation.
- Address generation.
- Order generation.
- Order-item generation.
- Payment-attempt generation.
- Reference-data resolution.
- Customer graph validation.

The original milestone focused on generating and validating a customer graph in memory. Its historical verification is recorded in Section 8.

### Milestone 2: Parallel Generation and JSONL Streaming

**Status: Implemented**

The generation pipeline was extended with:

- Chunk-based customer generation.
- Deterministic chunk seed derivation.
- Bounded parallel execution.
- Ordered consumption of chunk results.
- Lazy chunk planning and input consumption.
- JSON Lines streaming.
- Run output publication.

#### Chunk planning

`GenerationChunkPlanner` produces contiguous customer-ordinal ranges.

The planner:

- Starts customer ordinals at 1.
- Produces chunks up to the configured chunk size.
- Uses an iterator rather than allocating all chunk descriptors in advance.
- Handles a final partial chunk when the total customer count is not divisible by the chunk size.

#### Parallel execution

`ParallelChunkExecutor` uses a fixed-size thread pool and an `ExecutorCompletionService`.

Its implementation provides:

- A configured worker parallelism.
- A maximum number of submitted-but-not-yet-consumed tasks controlled by `maxInFlight`.
- Lazy consumption of the input iterator.
- Concurrent execution of generation tasks.
- Buffering of completed results until earlier inputs are ready.
- Consumption of results in their original input order.
- Cancellation of pending tasks when a failure occurs.
- Executor shutdown and termination handling.

The distinction between concurrent execution and ordered consumption is important: tasks may finish in a different order from submission, but the consumer receives their results in input order.

#### JSONL output contract

The output contract contains five entity files:

- `customers.jsonl`
- `addresses.jsonl`
- `orders.jsonl`
- `order_items.jsonl`
- `payments.jsonl`

Address output is enriched with geographic names, including:

- Building
- Society
- Road
- Area
- City
- State
- Country

The application resolves the relevant geographic building data when producing the address export representation.

#### Reproducibility qualification

Reproducibility depends on more than a master seed. Relevant factors include:

- Generation configuration and selected scenario.
- Deterministic chunk partitioning.
- Chunk seed derivation.
- Random-stream allocation to chunks and generators.
- Execution and output ordering.
- Stable reference data.
- Random-number consumption sequence.
- Identifier allocation and generator lifecycle.

A fixed seed alone does not guarantee identical output under every parallel execution configuration.

Implementation of chunked execution and output publication should not be interpreted as proof of every reproducibility or large-scale performance requirement.

Cross-worker reproducibility has now been verified for a specific controlled configuration using `ParallelGenerationReproducibilityTest`.

The test held the master seed, customer count, chunk size, reference data, scenario, and maximum in-flight count constant while changing worker parallelism from 1 to 4. All five JSONL output files were byte-for-byte identical.

This verifies the tested configuration; it does not establish exhaustive reproducibility across all possible generation inputs or execution settings.

### Milestone 3: Scenario-Based Business Generation

**Status: Implemented**

The generator supports three configurable business scenarios, loaded from `conf/hocon/scenario.conf`.

| Scenario | Orders per Customer | Items per Order | Product Selection |
|---|---:|---:|---|
| `baseline` | 2 | 2 | Uniform |
| `high_demand` | 4 | 4 | Weighted |
| `payment_failures` | 2 | 2 | Uniform |

#### Payment outcome configuration

Payment outcome weights are configurable independently for each scenario.

The four outcomes are:

1. Success on first attempt.
2. Success after one retry.
3. Success after two retries.
4. Permanent failure.

The baseline and high-demand scenarios use these weights, in the order above:

- `0.70`
- `0.20`
- `0.07`
- `0.03`

The payment-failures scenario uses:

- `0.45`
- `0.25`
- `0.15`
- `0.15`

The configured weights in all three supplied profiles sum to 1.0.

The configuration model checks that weights are finite and non-negative and that at least one weight is positive. The model does not itself enforce that the weights sum to 1.0.

#### Product selection

- `baseline` uses uniform product selection.
- `high_demand` uses weighted product selection across configured product reference data.
- `payment_failures` uses uniform product selection.

The high-demand configuration defines explicit selection weights for ten products, PROD_001 through PROD_010. The scenario loader validates these product IDs against the loaded product reference data.

The scenario loader validates that weighted product selection references known products and that at least one configured product weight is positive.

#### Integration

Scenario configuration is integrated into:

- Customer graph generation.
- Order and order-item cardinality.
- Order-item product selection.
- Payment-attempt outcome generation.

**Scope clarification:** These are business-generation scenarios. They vary order volume, basket size, product selection, and payment outcome weights.

They do not yet provide comprehensive data-quality profiles, statistically realistic customer or market behavior, dirty-data generation, or configurable scenario composition. Those remain future roadmap items.

## 5. Generation configuration and defaults

The application uses a generation run configuration to control the overall generation workload and execution behavior.

The previously inspected defaults are:

| Setting | Default |
|---|---:|
| Master seed | `42L` |
| Total customers | `100L` |
| Chunk size | `25L` |
| Parallelism | `2` |
| Maximum in-flight tasks | `4` |

The configuration enforces the following constraints:

- Master seed must be non-negative.
- Total customers must be non-negative.
- Chunk size must be positive.
- Parallelism must be positive.
- Maximum in-flight tasks must be greater than or equal to parallelism.

Recheck the current configuration implementation before treating these values as an exhaustive description of all supported options.

## 6. Latest reported verification

The latest supplied test output for the tested working tree reported:

| Measure | Result |
|---|---:|
| Tests run | 101 |
| Suites completed | 30 |
| Tests succeeded | 101 |
| Tests failed | 0 |
| Suites aborted | 0 |

Verification commands:

- `sbt "testOnly com.shopsphere.datagen.common.parallel.ParallelGenerationReproducibilityTest"` — 1 test passed.
- `sbt "Test / testOnly *"` — 101 tests passed across 30 suites.

The reproducibility test compared all five JSONL output files byte-for-byte using:
- Master seed: `42`
- Total customers: `53`
- Chunk size: `10`
- Run A: parallelism `1`, max in-flight `8`
- Run B: parallelism `4`, max in-flight `8`

The exact Git revision at test time was `62bf6fae2c923b331ed12314497757b7ff4c2153`. The reproducibility test was present as an untracked file and was not part of that commit.

This establishes that the tested working tree passed the reported test run. It does not establish reproducibility for every possible configuration or workload.

Run the complete test suite after subsequent source or configuration changes before making a new verification claim.

## 7. Canonical entity identifier contract

Decision record: [ADR-0001: Entity Identifier Conventions](decisions/ADR-0001-identifier-conventions.md) — Accepted.

The canonical identifier formats are:

| Entity | Format | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Caller-supplied ordinal |
| Order | `ORD000001_CUST000001` | Per customer |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

### Identifier rules

- Do not put an underscore between an entity prefix and its six-digit, zero-padded sequence.
- Use underscores to separate hierarchical identifier components.
- Child identifiers embed their relevant parent identifier.
- Explicit relationship fields, such as customer and order identifiers, remain required.
- Order and address sequences are scoped to the customer generator instance.
- Order-item and payment sequences are keyed by order ID.
- Concurrent identifier generation must preserve required uniqueness and parent-scoped sequence behavior.

An earlier source and test scan found no remaining matches for the searched legacy patterns `CUST_`, `ORD_CUST_`, `ADDR_`, `ITEM_`, and `PAY_` under `src/main/scala` and `src/test/scala`.

This was a point-in-time scan, not a guarantee about external datasets, generated files, or future changes.

## 8. Historical verification records

The records in this section preserve earlier verification evidence. Test counts refer to the specific working trees tested at the time and are not cumulative counts.

### Milestone 1 verification — 2026-10-22

Reported commands:

- `sbt compile` — passed.
- `sbt "Test / testFull"` — 57 tests passed across 20 suites, with zero failures.
- `sbt run` — completed successfully.

Observed application output included:

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

The payment count was determined by payment-attempt outcomes, not by a fixed payment-count parameter.

The supplied command output did not establish the exact Git revision tested.

### Identifier implementation verification

An earlier verification record reported:

- `sbt test`
- 14 tests succeeded across 7 suites, with zero failures.
- Scala test compilation completed successfully using Scala 2.13.18.
- `git diff --check` reported no whitespace errors.
- The source/test scan for the listed legacy identifier patterns returned no matches.

These results applied to the working tree subsequently committed as `bd19e78` (`feat: implement hierarchical entity identifiers`).

### Sequential JSONL output verification — 2026-10-24

An earlier verification record reported:

- `sbt compile` — passed.
- `sbt "Test / testFull"` — 71 tests passed across 25 suites, with zero failures.
- `sbt run` — completed successfully.
- `sbt "run data/exports"` — completed successfully with a custom output root.
- All five expected JSONL files were present.
- JSONL records were parsed successfully.
- Address output included building, society, road, area, city, state, and country names.
- Integration tests covered generation, validation, serialization, publication, record counts, and enriched address fields.
- Failure-path tests covered serialization failures, temporary-output cleanup, and invalid output-root behavior.

The recorded revision was `7ea61dcecf3a03c8082f3129c9ffff5d0eb34e39` (`Harden lifecycle validation and output publishing`).

### Parallel generation and scenario implementation

The implementation subsequently expanded to include:

- Chunk-based generation.
- Bounded parallel execution.
- Deterministic chunk seed derivation.
- Ordered result consumption.
- JSONL output publication.
- Configurable business scenarios.

The reported 100-test result across 29 suites belongs to the tested working tree for this later implementation. The exact Git revision was not included in the supplied test output.

Re-run the complete suite after subsequent source or configuration changes before treating that result as verification of a later revision.

## 9. Reproducibility and validation principles

`RandomGenerator` wraps `java.util.Random` initialized with a `Long` seed.

Reproducibility depends on more than a seed. Relevant factors include:

- Generation configuration and selected scenario.
- Deterministic chunk partitioning.
- Random-stream allocation to chunks and generators.
- Execution and output ordering.
- Stable reference data.
- The random-number consumption sequence.
- Identifier allocation and generator lifecycle.

The project should state reproducibility guarantees precisely and only to the extent verified by automated tests.

### Structural validation versus statistical validation

Graph validation and statistical realism are different responsibilities.

- **Structural validation** checks entity relationships, identifiers, required fields, and generation invariants.
- **Statistical validation** checks whether generated distributions, correlations, frequencies, and business metrics behave as intended.

Passing structural validation does not, by itself, establish statistical realism.

## 10. Current known gaps and review items

The following items require review or explicit verification before being considered complete:

- Define statistical expectations and acceptance thresholds for scenario-specific distributions.
- Evaluate whether order counts, basket sizes, product popularity, customer behavior, and payment outcomes are sufficiently realistic.
- Identify important correlations among customers, products, orders, payments, and time.
- Review lifecycle boundary behavior, including cases where an `asOfDate` precedes registration or another relevant lifecycle boundary.
- Review behavior for inaccessible output paths and invalid command-line arguments.
- Expand payment identifier tests if the model supports multiple payments per order in new ways.
- Recheck the current implementation of deterministic parallel generation and its tests against ADR-0003.
- Verify the `main-gradle` branch independently.
- Record exact Git revisions and test results for future verification.
- Evaluate large workloads, resource usage, and generation performance before making production-readiness claims.

## 11. Roadmap alignment and next steps

The project roadmap distinguishes the following later milestones.

### M4 — Realistic Dataset

**Status: Planned**

Planned scope:

- Configurable distributions.
- Configurable cardinality.
- Long-tail behavior.
- Important correlations.
- Statistical validation and measurable acceptance thresholds.

### M5 — Scenario Dataset

**Status: Planned**

Planned scope:

- Clean, slightly dirty, and dirty data profiles.
- Skew scenarios.
- Scenario composition.
- Scenario behavior and data-quality validation.

The implemented business scenarios (`baseline`, `high_demand`, and `payment_failures`) should not be assumed to fulfill every acceptance criterion of M5. They provide scenario-based business-generation behavior, but scenario composition, data-quality profiles, and dirty-data generation must be assessed separately.

### M6 — Production-Quality Generator

**Status: Planned**

Planned scope:

- A documented deterministic-output contract.
- A run manifest.
- Comprehensive validation.
- Observability and diagnosable failures.
- Reliable large-dataset generation.

### M7 — Spark Performance Laboratory Ready

**Status: Planned**

Planned scope:

- Configurable physical file layout.
- Reproducible file-size and skew scenarios.
- Documented dataset characteristics.
- Benchmark metadata.
- An understanding of generation performance.

### Immediate next steps

1. Finish reconciling the architecture, data model, project state, and ADRs against the current source, configuration, and tests.
2. Correct stale statements about milestone status, application wiring, writer class names, command-line behavior, and verification results.
3. Define measurable M4 acceptance criteria for distributions, cardinality, long-tail behavior, important correlations, and statistical validation.
4. Establish baseline metrics and statistical expectations before implementing realism changes.
5. Implement and test M4 incrementally, preserving scenario behavior, structural validation, the documented reproducibility contract, and JSONL output compatibility.
6. Run the complete SBT test suite after the final source or configuration changes, and record the exact Git revision alongside the results.
7. Verify the Gradle branch separately if branch parity or Gradle verification is required.

Documentation changes should remain separate from unrelated application refactoring. Before committing, review the complete diff, run `git diff --check`, validate Markdown links, and confirm `git status -sb`.

Do not attribute test results to a commit unless the tested revision is known.

## 12. Scope boundaries

The project is a data generator intended to produce configurable, relationally consistent e-commerce datasets.

The following should be treated as separate capabilities requiring their own scope and verification:

- Database persistence.
- Additional output formats beyond the established JSONL contract.
- Statistical realism beyond the distributions and relationships currently implemented.
- Dirty-data profiles and explicit data-quality scenarios.
- Production-scale performance claims.
- Downstream Spark benchmarking and data engineering laboratory setup.

The presence of a model, configuration option, or component does not alone establish that its behavior is fully integrated, tested, statistically realistic, or production-ready. Record those states separately and verify each claim against the current implementation.
