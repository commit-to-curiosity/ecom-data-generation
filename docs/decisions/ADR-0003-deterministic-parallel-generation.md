# ADR-0003: Deterministic Parallel Generation

- Status: Accepted
- Date: 2026-10-14

## Context

ShopSphere uses a seeded `RandomGenerator` backed by `java.util.Random`.

The customer graph generator passes random-generation state to domain generators. A fixed seed alone does not guarantee identical output when parallel scheduling changes the order in which random values are consumed.

Parallel generation must preserve the accepted entity identifier contract in [ADR-0001](ADR-0001-identifier-conventions.md). Customer identity allocation, parent-scoped sequences, random state, and output ordering must not depend on worker scheduling when reproducible output is required.

The implementation targets Scala 2.13 and must remain compatible with the project's SBT development branch.

## Decision

### 1. Random stream scope and chunk planning

1. Use one independent random stream per logical customer chunk.
2. Set the initial production target chunk size to 10,000 customers. This is a design target, not the current application default; the current default is configured through `GenerationRunConfig`.
3. Assign customers to chunks using deterministic global customer ordinals.
4. Derive each chunk's seed deterministically using the SplitMix64-style finalizer defined below.
5. Create chunk-local random generators and mutable generator instances. Do not share mutable random state across concurrently executing chunks.
6. Generate customers within each chunk in ascending ordinal order.
7. Use bounded parallel execution and bounded in-flight work.
8. Consume and write or merge chunk outputs in deterministic order.

#### Chunk seed derivation

For master seed `M` and zero-based chunk index `i`, calculate:

```text
input = M + GoldenGamma * (i + 1)
```

Apply the following 64-bit mixing function:

```text
GoldenGamma = 0x9E3779B97F4A7C15
Mix1        = 0xBF58476D1CE4E5B9
Mix2        = 0x94D049BB133111EB

mix64(x):
    x = (x ^ (x >>> 30)) * Mix1
    x = (x ^ (x >>> 27)) * Mix2
    return x ^ (x >>> 31)

chunkSeed = mix64(input)
```

All arithmetic uses 64-bit `Long` overflow, equivalent to arithmetic modulo 2^64. The unsigned right-shift operator `>>>` is used in the mixing function.

The implementation rejects chunk indices that are negative or greater than or equal to `Long.MaxValue`.

The constants and formula are part of the reproducibility contract and must not change silently.

Implementation: `ChunkSeedDerivation.scala`.

Verification: `ChunkSeedDerivationTest.scala`.

The seed-derivation test includes these reference values:

- `derive(42L, 0L) = -4767286540954276203L`
- `derive(42L, 1L) = 2949826092126892291L`

The algorithm is a SplitMix64-style seed-mixing finalizer; it is not the complete SplitMix64 random-number generator.

### 2. Identifier allocation

- Customer IDs use explicit global customer ordinals.
- Order and address sequences remain scoped to a customer.
- Order-item and payment sequences remain scoped to an order.
- Identifier formats must follow [ADR-0001](ADR-0001-identifier-conventions.md).
- Identifier allocation must preserve uniqueness and parent relationships independently of worker scheduling.

### 3. Bounded execution

The parallel execution layer must:

- Limit the number of chunks submitted but not yet consumed.
- Avoid creating an unbounded queue of generated work.
- Consume results in deterministic input order.
- Propagate generation failures to the caller.
- Cancel pending work and shut down execution resources when a failure interrupts the pipeline.
- Avoid treating a partially completed generation run as a successful completed run.

The implementation should be assessed against these requirements through source inspection and tests. Bounded in-flight work does not, by itself, guarantee a fixed total memory footprint for all workloads.

### 4. Output ordering and publication

- Entity records must be written in the deterministic order established by the generation pipeline.
- Chunk completion order must not determine final output order.
- The JSONL output contract must remain consistent with the architecture and data-model documentation.
- Output publication must preserve the documented behavior for completed runs, failures, cleanup attempts, and existing run directories.

This decision does not require the publisher to provide filesystem transactionality or guaranteed atomic publication on every filesystem.

### 5. Reproducibility contract

The intended reproducibility contract is scoped to fixed inputs and fixed generation rules. Relevant inputs include:

- Master seed.
- Selected scenario and generation configuration.
- Total customer count and chunk size.
- Chunk seed-derivation algorithm.
- Generator and reference-data versions.
- Identifier-allocation rules.
- Random-number consumption order.
- Output ordering and serialization behavior.

Worker count is an execution parameter and must not affect generated output when all other reproducibility inputs and generation rules remain unchanged. This guarantee must be verified by comparing output across different worker counts.

Changes to the inputs or rules above may change generated values. Such changes should be documented as changes to the reproducibility contract where relevant.

### 6. Verification requirements

Tests should cover:

1. Deterministic chunk boundaries for the same generation inputs.
2. Stable global customer ordinals across chunks.
3. Deterministic chunk seed derivation.
4. Correct parent-scoped identifier sequences.
5. Ordered consumption of chunk results when chunks complete out of order.
6. Propagation of generation failures and cancellation of pending work.
7. Output publication behavior after successful generation and after failures.
8. Repeated runs with the same seed, scenario, configuration, and execution settings.
9. Equality across worker counts, with all other reproducibility inputs held constant.
10. Referential integrity and identifier uniqueness across a complete generated dataset.

Cross-worker-count reproducibility requires an integration test that compares generated output under different worker counts while holding all other reproducibility inputs constant.

A test result should be recorded with the exact Git revision tested. Passing a general unit-test suite does not establish all of the requirements above unless those behaviors are specifically covered.

## Consequences

- Chunk planning and customer ordinal allocation are explicit parts of the generation contract.
- Random state is isolated per logical chunk rather than shared across concurrently executing chunks.
- Bounded execution limits in-flight work and supports controlled resource usage.
- Output ordering is independent of chunk completion order.
- Reproducibility claims must be defined in terms of fixed inputs and verified execution guarantees.
- Changes to chunking, seed derivation, identifier allocation, random-number consumption, or output ordering may change generated values and require regression tests.
- The production target of 10,000 customers per chunk must not be confused with the current application default.
- Large-scale performance and memory behavior require separate measurements; they cannot be established solely by the design.

## Alternatives considered

1. **Share one mutable random generator across all worker tasks.** Rejected because random-number consumption can depend on scheduling and can make results difficult to reproduce.
2. **Allow chunks to write directly in completion order.** Rejected because output order could vary with execution timing.
3. **Submit all chunks without a bound.** Rejected because the number of pending tasks and retained results could grow with the total workload.
4. **Allocate child identifiers from a single global sequence.** Rejected because it conflicts with the parent-scoped identifier contract in ADR-0001.
5. **Treat a fixed master seed as sufficient for reproducibility.** Rejected because deterministic output also depends on chunk planning, random-stream allocation, identifier allocation, and output ordering.

## Implementation status and evidence

Chunk-based generation, deterministic chunk seed derivation, bounded parallel execution, ordered result consumption, and JSONL output publication have been implemented in the generation pipeline.

The documented default run configuration is 100 customers, a chunk size of 25, parallelism of 2, and a maximum of 4 in-flight chunks. The production target of 10,000 customers per chunk is a separate design target.

A full SBT test run was reported as passing on 2026-10-25:

- Tests run: 100
- Suites completed: 29
- Failures: 0
- Aborted suites: 0

The exact Git revision tested was not included in the supplied test output. These results are historical evidence for the tested working tree, not proof that every requirement in this ADR has been verified.

The seed derivation tests verify repeatability for identical inputs, known algorithm outputs, distinct outputs for the tested chunk indices and master seeds, and rejection of invalid chunk indices.

The chunk generator tests verify repeatability for the same chunk and master seed, ascending customer ordinal generation, and different generated streams for the tested chunk indices.

Cross-worker-count byte-for-byte output equality should be considered verified only if a test explicitly compares the relevant outputs under different worker counts with all other contract inputs held constant.

### Cross-worker reproducibility verification — 2026-11-13

**Status: Verified for the tested configuration.**

`ParallelGenerationReproducibilityTest` compares the byte contents of all five generated JSONL files while varying worker parallelism and keeping the other tested reproducibility inputs constant.

| Parameter | Run A | Run B |
|---|---:|---:|
| Master seed | 42 | 42 |
| Total customers | 53 | 53 |
| Chunk size | 10 | 10 |
| Parallelism | 1 | 4 |
| Max in-flight | 8 | 8 |

The following files were compared byte-for-byte:
- `customers.jsonl`
- `addresses.jsonl`
- `orders.jsonl`
- `order_items.jsonl`
- `payments.jsonl`

All comparisons passed.

Verification:
- Focused reproducibility test: 1 passed.
- Full SBT test suite: 101 tests passed across 30 suites, zero failures.
- Full-suite command: `sbt "Test / testOnly *"`

The test was run against baseline revision `62bf6fae2c923b331ed12314497757b7ff4c2153`, with the reproducibility test present as an untracked working-tree file.

This verifies worker-count independence for the tested configuration. It does not establish exhaustive reproducibility across all possible inputs, chunk sizes, scenarios, or workloads.

## Related documentation

- [Identifier conventions (ADR-0001)](ADR-0001-identifier-conventions.md)
- [Architecture](../ARCHITECTURE.md)
- [Data model](../DATA_MODEL.md)
- [Project state](../PROJECT_STATE.md)
