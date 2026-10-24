# Architecture

## 1. Purpose and scope

ShopSphere E-Commerce Data Generator is a Scala 2.13.18 application for generating configurable, relationally consistent e-commerce data.

The application supports customer graph generation, reference-data loading, scenario-specific business behavior, chunk-based generation, bounded parallel execution, JSON Lines (JSONL) output, and run output publication.

This document describes component responsibilities and the execution pipeline. Implementation, unit-test coverage, application wiring, end-to-end verification, and statistical realism are distinct properties and must not be treated as interchangeable.

## 2. Architectural layers

### 2.1 Configuration

Package: `com.shopsphere.datagen.common.config`

Responsibilities include:

- Reading application configuration.
- Providing format-specific readers for HOCON, JSON, and YAML.
- Creating readers through `ConfigReaderFactory`.
- Loading typed configuration through configuration-loader implementations.

Customer-specific configuration is defined under `customer.config`. Loader implementations reside under `customer.loader`, with `CustomerLoaderFactory` creating the customer configuration loader.

Generation scenarios are defined under `com.shopsphere.datagen.scenario`. `GenerationScenarioLoader` loads scenario definitions, and `GenerationScenario` represents the settings consumed by the generation pipeline.

The implemented scenario names are:

- `baseline`
- `high_demand`
- `payment_failures`

Scenario selection and default handling are described in Section 3.1.

### 2.2 Shared generation infrastructure

Package: `com.shopsphere.datagen.common`

Important components include:

- `generator.Generator[T]`: common generator abstraction.
- `distribution.RandomGenerator`: seeded random-number generation backed by `java.util.Random`.
- `distribution.Distribution[T]`: distribution abstraction.
- `UniformDistribution[T]`: uniform selection.
- `WeightedDistribution[T]`: weighted selection. Its accepted weight values and validation behavior are defined by the implementation and tests.
- `TriangularDistribution`: triangular distribution implementation.
- `json.JsonMapper`: shared JSON serialization support.
- `output`: JSON Lines writing and run output publication.
- `enums`: domain values and configuration-format types.

Distribution implementations allow generators to express selection behavior without embedding all selection logic directly in domain models.

A fixed seed alone does not guarantee identical output across different execution schedules, worker counts, partitioning strategies, or random-number consumption sequences. Reproducibility claims must account for the complete generation execution model.

### 2.3 Customer generation

Package: `com.shopsphere.datagen.customer`

Important components include:

- `CustomerConfig` and related configuration models.
- `CustomerConfigLoader` and supporting configuration loaders.
- `CustomerIdentityGenerator`: generates customer identity attributes.
- `CustomerIdGenerator`: generates canonical customer identifiers.
- `CustomerGenerator`: generates a `Customer`.
- `CustomerChunkGenerator`: generates a chunk of customer graphs.
- `CustomerGraphGenerator`: coordinates related entities for one customer.
- `CustomerGraphValidator`: validates graph relationships and generation invariants.
- `Customer` and `CustomerGraph`: represent a customer and its associated generated entities.

Customer configuration includes concerns such as demographics, lifecycle, status, segmentation, acquisition, and behavior.

Chunk-based generation allows the application to process customers in logical batches instead of requiring the entire dataset to be retained as one in-memory collection. It does not, by itself, guarantee a fixed upper bound on total memory usage: completed results may wait for earlier chunks, and each chunk may contain a materialized collection of generated graphs.

### 2.4 Reference data

Geography components reside under `com.shopsphere.datagen.geography`:

- `GeographyLoader` loads geography reference data.
- `GeographyModels` and `GeographyJsonModels` represent geography data.
- `GeographyReferenceData` provides reference data to consumers.
- `GeographyValidator` validates geographic relationships.
- `BuildingLocation` supports building-location data.

Product components reside under `com.shopsphere.datagen.product`:

- `ProductLoader` loads product data.
- `Product` and `ProductsData` represent product data.
- `ProductReferenceData` provides products to generators.

The application loads reference data and passes it to the relevant generation and validation components. Product selection can use uniform or weighted selection according to the selected scenario.

Reference data must be valid before it is used to generate dependent entities.

### 2.5 Domain generation

| Domain | Main components | Responsibility |
|---|---|---|
| Customer | `CustomerGenerator`, `CustomerIdentityGenerator` | Generate customer records and identity attributes |
| Address | `AddressGenerator`, `Address` | Generate customer addresses using geography reference data |
| Order | `OrderGenerator`, `Order` | Generate orders associated with customers |
| Order item | `OrderItemGenerator`, `OrderItem`, `OrderItemIdGenerator` | Generate order lines and select products |
| Payment | `PaymentGenerator`, `PaymentIdGenerator` | Generate payment records associated with orders |
| Payment outcome | `PaymentAttemptOutcomeGenerator` | Generate payment-attempt outcomes and associated statuses |
| Customer graph | `CustomerGraphGenerator`, `CustomerGraphValidator` | Assemble and validate related entities |

The number of addresses, orders, and order items is controlled by generation settings. Payment counts depend on the payment-attempt outcomes produced by the configured payment behavior; they are not necessarily a fixed number per order.

### 2.6 Scenario layer

Package: `com.shopsphere.datagen.scenario`

The scenario layer separates business behavior profiles from the generation pipeline.

Scenario definitions provide settings consumed by the relevant generators, including:

- Orders per customer.
- Items per order.
- Product selection strategy.
- Payment outcome weights.

The implemented scenarios are:

| Scenario | Intended behavior | Configurable behavior |
|---|---|---|
| `baseline` | Baseline generation behavior | Order and item counts, product selection, and payment outcome weights |
| `high_demand` | Higher order activity and larger baskets | Order and item counts and weighted product selection |
| `payment_failures` | Increased payment failure and retry behavior | Payment outcome weights and associated generation behavior |

The exact values and outcome definitions are specified in scenario configuration. They should not be inferred from the descriptions in this table.

These scenarios implement business-behavior variations. They do not, by themselves, establish that generated datasets reproduce real-world business distributions or satisfy separate requirements for clean, slightly dirty, and dirty data profiles.

## 3. Application entry point and execution flow

Source: `src/main/scala/com/shopsphere/datagen/Main.scala`

The application coordinates configuration loading, scenario selection, reference-data loading, customer generation, chunk execution, and output publication.

The logical execution flow is:

1. Read the application arguments, including the output root and requested scenario.
2. Resolve the scenario, using the configured default when the scenario argument is omitted or blank.
3. Load application and customer configuration.
4. Initialize the generation-run settings and random-generation infrastructure.
5. Load geography reference data and validate its integrity.
6. Load product reference data.
7. Initialize the customer graph-generation components with the required configuration and reference data.
8. Plan and execute customer generation in chunks.
9. Apply scenario-specific settings for order counts, basket sizes, product selection, and payment outcomes.
10. Validate generated customer graphs through the configured graph-generation and validation path.
11. Stream generated entity records to the five JSONL output files.
12. Publish the completed run output and report the output location and record counts.

The exact division of these responsibilities among `Main`, chunk generation, graph generation, validation, and output components should remain aligned with the source code.

### 3.1 Command-line behavior

The application accepts two positional command-line arguments:

1. **Output root** — the first argument; defaults to `data/raw` when omitted.
2. **Scenario name** — the second argument; when omitted or blank, the application loads the default scenario specified by `scenarios.default` in `conf/hocon/scenario.conf`.

The generation-run settings are currently initialized through `GenerationRunConfig()` in `Main.scala`:

| Setting | Default |
|---|---:|
| Master seed | `42L` |
| Total customers | `100L` |
| Chunk size | `25L` |
| Parallelism | `2` |
| Maximum in-flight chunks | `4` |

These run settings are separate from the scenario configuration. Scenario definitions configure business-generation behavior, including order counts, item counts, product selection, and payment outcomes.

Scenario names and configuration values are resolved through the configuration-loading path. Invalid scenario names and configuration values should be rejected; document the precise failure behavior only where it is confirmed by the implementation and tests.

### 3.2 Output contract

Each published run contains five JSONL files:

| File | Contents |
|---|---|
| `customers.jsonl` | Customer records |
| `addresses.jsonl` | Address records enriched with geographic names |
| `orders.jsonl` | Order records |
| `order_items.jsonl` | Order-item records |
| `payments.jsonl` | Payment records |

The serialization contract includes:

- UTF-8 output.
- One JSON object per line.
- snake_case JSON property names.
- ISO-formatted date strings.
- JSON numeric representation for `BigDecimal` values.
- Entity identifiers and explicit relationship fields.
- Geographic names in exported address records, including building, society, road, area, city, state, and country.

The five-file contract should remain compatible as scenario behavior evolves unless a separate, explicit decision changes it.

## 4. Customer graph generation

Source: `customer/generator/CustomerGraphGenerator.scala`

`CustomerGraphGenerator` coordinates the creation of a customer's related entities. Its dependencies include customer data, geography and product reference data, random-generation infrastructure, identifier generators, and the applicable as-of date.

The graph-generation process coordinates the creation of:

1. Customer addresses.
2. Customer orders.
3. Order items associated with generated orders.
4. Payment records associated with generated orders.
5. A `CustomerGraph` containing the generated entities.

The number of generated entities depends on the supplied generation settings and scenario behavior. Payment counts depend on payment-attempt outcomes.

`CustomerGraphValidator` is responsible for checking graph relationships and generation invariants. Validation guarantees should be described in terms of the actual checks performed by that component and the point at which the application invokes it.

## 5. Chunk planning and parallel execution

### 5.1 Chunk planning

`GenerationChunkPlanner` divides the requested customer ordinal range into deterministic chunks based on the configured total customer count and chunk size.

Global customer ordinals provide stable customer identity inputs. Chunk boundaries depend on the total count and configured chunk size.

The configured chunk size is an application setting. ADR-0003 records a separate initial production target of 10,000 customers per chunk; that target is not the same as the current default of 25 customers.

### 5.2 Chunk generation

`CustomerChunkGenerator` generates the customer graphs belonging to a chunk. Chunk generation uses the run configuration and scenario settings passed into the pipeline.

Chunk-local generation state and seed derivation are important to reproducibility. The guarantees depend on the implemented algorithm, configuration, random-number consumption, and tests.

### 5.3 Bounded parallel execution

`ParallelChunkExecutor` executes chunk work using a fixed thread pool and limits the number of submitted chunks that have not yet been consumed.

The executor consumes results in input order and buffers completed out-of-order results until they can be consumed in that order. On a generation failure, it cancels pending work and propagates the failure to the caller. Resource shutdown behavior is governed by the executor implementation.

This design bounds in-flight chunk work. It does not imply that every generated graph is processed immediately upon completion or that memory usage is independent of chunk size and workload.

### 5.4 Output consumption

The consuming thread writes the results to the JSONL output writer. Parallel generation and output writing are separate responsibilities.

Output ordering is determined by the order in which chunk results are consumed and records are written. Any stronger claim about byte-for-byte reproducibility must be supported by the actual implementation and tests.

## 6. Randomness and reproducibility

`RandomGenerator` wraps `java.util.Random` initialized with a `Long` seed.

A fixed seed alone does not guarantee identical output across arbitrary worker counts, chunk sizes, scheduling patterns, or changes to the random-number consumption sequence.

Reproducibility depends on factors including:

- Master seed and scenario configuration.
- Deterministic chunk boundaries.
- Deterministic seed derivation for each chunk.
- Random-stream allocation and generator state.
- Customer ordinal and identifier allocation.
- Stable reference data.
- Generation order and random-number consumption.
- Output ordering and serialization behavior.

ADR-0003 records the accepted design for deterministic parallel generation. Its requirements should not be treated as verified guarantees unless the corresponding implementation and tests establish them.

## 7. JSONL writing and run publication

### 7.1 JSONL stream writer

`JsonLinesStreamWriter` writes entity records incrementally as UTF-8 JSONL.

Its responsibilities include:

- Serializing each entity record as a single JSON line.
- Opening output files lazily when first written, using `CREATE_NEW` and `WRITE` options.
- Writing records incrementally rather than retaining the entire output dataset in memory.
- Maintaining record counts for each output entity type.
- Propagating serialization and file-writing failures.
- Managing output writers for the duration of the generation run.

The writer is intended for single-threaded consumption. Parallel chunk generation is handled separately by `ParallelChunkExecutor`; output writing is performed by the consuming thread.

`JsonLinesWriter` is a separate writer implementation. The main generation flow uses `JsonLinesStreamWriter`.

The shared JSON mapper defines the serialization conventions used by exported records.

### 7.2 Run output publisher

`RunOutputPublisher` stages generated output in a temporary directory and publishes the completed run under a run-specific directory named `run_<runId>`.

Its documented behavior includes:

1. Creating a temporary output location.
2. Executing the writer operation.
3. Publishing completed output under the final run directory.
4. Attempting to clean up temporary output if writing fails.
5. Propagating failures to the caller.
6. The behavior when the target run directory already exists is implementation-defined and should be verified against the publisher and its tests.

The default output root is `data/raw`; a custom output root can be supplied as the first command-line argument.

The publication mechanism must not be described as providing full filesystem transactionality or guaranteed atomic publication across every filesystem and operating environment unless those properties have been specifically established.

### 7.3 Failure handling

Generation, validation, serialization, and output-publication failures should not be silently converted into successful runs.

The publisher's cleanup behavior and existing-target handling are described in the implementation and testing history. Inaccessible-path behavior and broader command-line validation should remain identified as review items unless subsequently verified.

## 8. Verification and status interpretation

When documenting a capability, distinguish these states:

1. Component exists.
2. Component has unit-test coverage.
3. Component is wired into the application.
4. Application-level behavior has been exercised.
5. The relevant guarantee has been verified by a test that directly checks it.

A passing test suite does not automatically establish every architectural, statistical, performance, or reproducibility guarantee.

Historical test counts and dates belong in `PROJECT_STATE.md` or the relevant decision record. They should be attributed to a specific commit only when the tested revision is known.

## 9. Related documentation

- [Project state](PROJECT_STATE.md)
- [Documentation index](README.md)
- [Decision records](decisions/README.md)
- [Identifier conventions (ADR-0001)](decisions/ADR-0001-identifier-conventions.md)
- [Build-system branch strategy (ADR-0002)](decisions/ADR-0002-build-system-branch-strategy.md)
- [Deterministic parallel generation (ADR-0003)](decisions/ADR-0003-deterministic-parallel-generation.md)

## 10. Maintenance rule

Update this document when component responsibilities, constructor dependencies, configuration defaults, generation flow, output behavior, or application wiring change.

Verify descriptions against the current source and tests rather than relying on older summaries. Preserve the distinction between intended design, implemented behavior, tested behavior, and verified guarantees.