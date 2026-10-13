# ADR-0003: Deterministic Parallel Generation

- Status: Accepted
- Date: 2026-10-14

## Context

ShopSphere uses a seeded `RandomGenerator` backed by `java.util.Random`.
The customer graph generator passes random state to multiple domain
generators. A fixed seed alone does not guarantee identical output when
parallel scheduling changes the order in which random values are consumed.

Parallel generation must preserve the accepted entity identifier contract
in [ADR-0001](ADR-0001-identifier-conventions.md). Customer identity
allocation, parent-scoped sequences, random state, and output ordering
must not depend on worker scheduling.

The initial implementation targets Scala 2.13 and must remain compatible
with the project's existing SBT development branch.

## Decision

### Random stream scope and chunk planning

1. Use one independent random stream per logical customer chunk.
2. Set the initial production chunk size to 10,000 customers.
3. Assign customers to chunks using deterministic global customer ordinals.
4. Derive each chunk's seed deterministically from the master seed and
   chunk index using a documented 64-bit mixing algorithm.
5. Create chunk-local random generators and mutable generator instances.
   Do not share mutable random state across concurrently executing chunks.
6. Generate customers within each chunk in ascending ordinal order.
7. Use bounded parallel execution and bounded in-flight work.
8. Write or merge chunk outputs in a deterministic order.

The exact seed-derivation algorithm must be specified and tested in the
implementation before the reproducibility contract is considered verified.

### Identifier allocation

- Customer IDs use explicit global customer ordinals.
- Order and address sequences remain scoped to a customer.
- Order-item and payment sequences remain scoped to an order.
- Child IDs embed their parent IDs, and explicit relationship fields remain
  mandatory.
- Parallel scheduling must not determine which customer or parent receives
  a sequence number.

The identifier formats in ADR-0001 remain unchanged.

### Reproducibility guarantee

For a fixed master seed, configuration, reference data, generator version,
chunk size, seed-derivation algorithm, and generation logic, canonical output
must be identical across worker counts, including 1, 4, and 8 workers.

This decision does not promise byte-for-byte reproducibility across arbitrary
generator versions or changes to chunk size, seed derivation, reference data,
or random-consumption order. Such changes may alter generated values and
must be treated as reproducibility-contract changes.

## Consequences

- Chunk boundaries and customer ordinals become part of the reproducibility
  contract.
- Each chunk can be scheduled independently without sharing mutable RNG state.
- Changing the chunk size can change generated values.
- Changing random draw order within a chunk can change subsequent values.
- Bounded execution and streaming or chunked output are required to control
  memory use at large dataset sizes.
- Worker-count invariance must be established by tests, not inferred from
  thread-safe counters.
- The initial 10,000-customer chunk size is a starting point and may be
  revised following throughput and memory benchmarks.

## Alternatives considered

1. One shared random stream for all workers: rejected because concurrent
   consumption order can depend on scheduling.
2. One random stream per customer: not selected for this initial design;
   chunk-scoped streams are the chosen scope.
3. Unbounded parallel submission and accumulation: rejected because it can
   create excessive memory pressure for large datasets.
4. Relying only on atomic ID counters: rejected because thread-safe counters
   do not establish deterministic assignment or random output.

## Verification and acceptance criteria

Implementation must demonstrate:

- Deterministic chunk planning with no ordinal gaps or overlaps.
- Stable seed derivation for the same master seed and chunk index.
- Canonical output equality for worker counts 1, 4, and 8.
- Reproducibility across repeated runs with the same inputs.
- Correct identifier formats, parent-scoped counters, and referential integrity.
- Correct handling of partial final chunks and generation failures.
- Passing existing tests and new deterministic parallel-generation tests.

These are acceptance criteria, not claims that the implementation or tests
already exist or pass.

## Related documentation

- [Identifier conventions (ADR-0001)](ADR-0001-identifier-conventions.md)
- [Architecture](../ARCHITECTURE.md)
- [Project state](../PROJECT_STATE.md)
- [Data model](../DATA_MODEL.md)