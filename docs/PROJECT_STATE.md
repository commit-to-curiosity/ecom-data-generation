# Project State

**Last updated:** 2026-11-14

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
- `common.analysis`: dataset summary analysis and customer-profile statistical summaries over generated JSONL datasets.
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

Production-path reproducibility has also been verified through `MainIntegrationTest`.

The integration test invokes the application through `Main.run(args, runConfig)`, using an injected `GenerationRunConfig`. The normal `Main.main(args)` entry point continues to use the default configuration.

The production-path test compares all five JSONL output files byte-for-byte while holding the master seed, customer count, chunk size, and maximum in-flight count constant and changing parallelism from 1 to 4.

The integration suite also verifies:
- Default application execution.
- Zero-customer generation and creation of empty output files.
- Generation of 53 customers across six chunks, including a final partial chunk of three customers.

The lower-level reproducibility test and production-path integration test verify different layers of the generation pipeline. Both establish worker-count independence for their tested configuration; neither establishes exhaustive reproducibility for all possible inputs or workloads.

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

The latest supplied full-suite test output for the tested working tree reported:

| Measure | Result |
|---|---:|
| Tests run | 123 |
| Suites completed | 32 |
| Tests succeeded | 123 |
| Tests failed | 0 |
| Suites aborted | 0 |

### Verification

- Full SBT test suite: 123 tests passed across 32 suites, with zero failures or aborted suites.
- Full-suite command: `sbt "Test / testFull"`.
- `sbt test` is not the full-suite command in the current build configuration. It invokes `testQuick` and may report zero tests when there are no tests to run incrementally.
- `sbt "Test / fullTest"` is not a valid task in the current build configuration.

The full suite includes tests for:

- Customer, address, order, order-item, and payment generation.
- Configuration and scenario loading.
- Structural graph validation.
- Chunk planning and parallel execution.
- Cross-worker and production-path reproducibility.
- JSONL output and application integration.
- Dataset summary analysis.
- Customer-profile statistical summaries.

### Production-path integration coverage

`MainIntegrationTest` exercises the application generation pipeline with an injectable `GenerationRunConfig`.

The integration tests cover:

- Default application execution.
- Zero customers, with all five JSONL files created empty.
- Generation across multiple chunks, including a final partial chunk.
- Byte-for-byte equality of all five JSONL files when changing parallelism from 1 to 4.

The cross-worker comparison holds the master seed, customer count, chunk size, reference data, scenario, and maximum in-flight count constant while changing worker parallelism.

This verifies the tested configurations. It does not establish exhaustive reproducibility across all possible inputs, scenarios, or execution settings.

### Dataset analysis coverage

The new analysis package provides:

- Record counts across the five required JSONL entity files.
- Total record count with overflow-safe arithmetic.
- Customer-profile summaries for age, gender, customer status, customer segment, acquisition channel, acquisition campaigns by channel, preferred device, and preferred payment method.
- Age minimum, maximum, mean, median, and configured age-band frequencies.
- Category counts and proportions, including zero-count categories.
- Input validation for malformed JSON, non-object records, missing fields, unknown categories, invalid dates, and inconsistent campaign/channel combinations.

The analyzer reads JSONL records incrementally rather than loading the complete files into memory.

**Integration qualification:** The analysis API can analyze an existing run directory when supplied with an `asOfDate` and age-band definitions. It is not currently wired into the normal generation pipeline or CLI. It does not yet provide order, order-item, product, or payment statistical summaries, and it does not enforce statistical acceptance thresholds.

### Git verification context

The 123-test result applies to the working tree tested in the latest supplied test output.

Do not attribute this result to a commit unless the exact tested Git revision is independently established. Check the current repository using:

- `git status -sb`
- `git log -5 --oneline --decorate`

The result verifies successful execution of the reported test suite for that tested working tree. It does not establish million-customer-scale performance, statistical realism, or production readiness.

Run `sbt "Test / testFull"` after subsequent source or configuration changes before making a new verification claim.

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

### Cross-worker reproducibility verification — 2026-11-13

An earlier verification run reported:

- `ParallelGenerationReproducibilityTest`: 1 test passed.
- Full SBT test suite: 101 tests passed across 30 suites, zero failures.
- Full-suite command: `sbt "Test / testOnly *"`.

The tested working tree was based on revision `62bf6fae2c923b331ed12314497757b7ff4c2153`. The reproducibility test was present as an untracked file and was not part of that commit.

This is historical evidence for the earlier working tree. It is superseded as the latest verification result by the 104-test production-path verification recorded in Section 6.

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

The following items remain open and require implementation, review, or explicit verification:

- Define statistical expectations and measurable acceptance thresholds for the customer and business-generation distributions.
- Extend statistical summaries beyond customer profiles to order cardinality, basket sizes, product popularity, payment outcomes, and payment-attempt counts.
- Evaluate whether generated distributions and business behavior are sufficiently realistic, rather than merely structurally valid.
- Identify and model important correlations among customer attributes, products, orders, payments, and time.
- Review lifecycle boundary behavior, including cases where an `asOfDate` precedes registration or another relevant lifecycle boundary.
- Review behavior for inaccessible output paths and invalid command-line arguments.
- Expand reproducibility verification across additional scenarios, chunk sizes, and generation configurations.
- Verify the `main-gradle` branch independently.
- Benchmark analysis and generation resource usage on large workloads, including the planned 1,000,000-customer scale.
- Establish evidence for performance and memory behavior before making production-readiness claims.
- Consider whether statistical analysis should become an optional post-generation command or a separately invoked analysis workflow.

## 11. Roadmap alignment and next steps

The project roadmap distinguishes the following milestones.

### M4 — Realistic Dataset

**Status: In Progress**

M4 has two complementary objectives:

1. Support controlled synthetic datasets with explicit, configurable distributions and cardinalities.
2. Support more realistic e-commerce datasets through representative population distributions, business behavior, and meaningful correlations.

The controlled generation behavior must remain available. Realism improvements must preserve the established structural and reproducibility contracts unless a deliberate, documented decision changes them.

#### M4.1 — Statistical Measurement Framework

**Status: Implemented and tested**

M4.1 established the initial dataset analysis framework.

**M4.1.1 — Dataset Summary Analyzer**

Implemented:

- Incremental JSONL reading for the five required entity files.
- Record counts per entity file.
- Total record count.
- Input validation for required files, JSON syntax, and JSON object records.
- Overflow-safe record counting.

**M4.1.2 — Customer Profile Statistics**

Implemented:

- Customer age statistics: minimum, maximum, mean, and median.
- Configured age-band counts and proportions.
- Categorical counts and proportions for gender, customer status, customer segment, acquisition channel, preferred device, and preferred payment method.
- Conditional acquisition-campaign distributions by acquisition channel.
- Retention of zero-count categories.
- Validation of customer profile fields, categories, dates, age bands, and campaign/channel combinations.

The analysis API is tested by the full SBT test suite.

**Integration and scope qualification:**

- The analyzer operates on an existing generated run directory.
- Customer-profile analysis requires an externally supplied `asOfDate` and age-band definitions.
- The analysis API is not currently invoked by the normal generation pipeline or CLI.
- Statistical acceptance thresholds have not been established.
- Statistical summaries for orders, order items, products, and payments remain outstanding.
- Million-customer analysis performance has not yet been verified.

#### Remaining M4 work

The following areas remain planned:

- **M4.2 — Customer Population Realism:** Review and improve customer attribute distributions and important conditional relationships.
- **M4.3 — Variable Transaction Cardinality:** Support configurable order counts per customer and item counts per order, including appropriate distributions.
- **M4.4 — Product Popularity and Affinity:** Introduce long-tail product popularity and relevant product-selection relationships.
- **M4.5 — Payment Preferences and Chronology:** Evaluate payment-method preference linkage, payment outcomes, and chronological consistency of payment attempts.
- **M4.6 — End-to-End Statistical Validation:** Extend statistical summaries, define measurable expectations, and validate generated behavior across scenarios and seeds.
- **M4.7 — Large-Dataset Verification:** Evaluate generation and analysis at the planned 1,000,000-customer scale, recording runtime and resource usage.

These are planned work areas, not claims of existing implementation.

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

1. Review the current M4.1 analysis implementation and documentation for consistency.
2. Extend statistical analysis to order, order-item, product, and payment data.
3. Define measurable statistical expectations and acceptance thresholds using controlled baseline runs.
4. Establish baseline metrics across multiple seeds before changing generation distributions or random-number consumption.
5. Implement M4.2 through M4.5 incrementally, preserving existing scenario behavior, structural validation, reproducibility, and JSONL output compatibility.
6. Add end-to-end statistical validation and test it across relevant scenarios and seeds.
7. Benchmark generation and analysis at the planned 1,000,000-customer scale.
8. Run `sbt "Test / testFull"` after final source or configuration changes, and record the exact Git revision alongside the results.
9. Verify the Gradle branch separately if branch parity or Gradle verification is required.

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
