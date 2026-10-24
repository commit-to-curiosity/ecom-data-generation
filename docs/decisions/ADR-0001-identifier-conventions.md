# ADR-0001: Entity Identifier Conventions

- Status: Accepted
- Date: 2026-10-14

## Context

ShopSphere generates customers and related entities, including addresses, orders, order items, and payments.

Identifiers must be predictable, unique within their intended scope, and useful for tracing relationships between generated records. Earlier formats used inconsistent underscore placement and did not embed parent identifiers consistently.

The identifier contract must also remain compatible with chunk-based generation. Customer ordinals are supplied by the generation pipeline, while child identifiers use sequences scoped to their intended parent.

## Decision

Use the following canonical identifier formats:

| Entity | Example | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Caller-supplied ordinal |
| Order | `ORD000001_CUST000001` | Per customer |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

### Formatting rules

- Entity prefixes are immediately followed by six-digit, zero-padded sequence numbers.
- Six digits is the minimum formatted width, not a maximum supported sequence value.
- Underscores separate hierarchical identifier components.
- Child identifiers embed the relevant parent identifier.
- Explicit relationship fields remain mandatory; embedding a parent ID does not replace referential integrity.
- Customer ordinals are supplied by the caller and must be globally stable when global customer identity is required.
- Order and address sequences are scoped to the customer.
- Order-item and payment sequences are scoped to the order.

### Sequence lifecycle

Generator instances and counters must have the intended lifecycle so that sequences reset at the correct parent boundary.

Order and address counters must not accidentally continue across unrelated customers. Order-item and payment counters must not accidentally continue across unrelated orders.

The parallel generation pipeline must preserve identifier uniqueness and parent-scoped sequence behavior. For reproducible output, sequence allocation must not depend on nondeterministic worker scheduling.

## Consequences

- Generator implementations and tests must use the canonical formats.
- Generated records retain explicit relationship fields as well as hierarchical identifiers.
- Identifier tests should verify exact formatting and sequence behavior at parent boundaries.
- Existing generated datasets using legacy formats may require migration or regeneration.
- Parallel generation must allocate customer ordinals and child sequences consistently with this contract.
- Changes to the identifier contract require a deliberate decision and coordinated updates to generators, tests, data-model documentation, and downstream consumers.

## Alternatives considered

1. **Keep legacy formats.** Rejected because they do not meet the canonical hierarchical convention.
2. **Use globally increasing child sequences.** Rejected in favor of per-parent sequences.
3. **Embed parent IDs and retain explicit relationship fields.** Accepted because it improves traceability while preserving explicit relationships.

## Verification

The following test classes cover identifier construction and related behavior:

- `CustomerIdGeneratorTest` checks the canonical customer ID format.
- `AddressGeneratorTest` checks the canonical address ID and customer relationship.
- `OrderGeneratorTest` checks the canonical order ID format.
- `OrderItemGeneratorTest` checks order-item ID construction and parent-scoped sequence behavior.
- `PaymentGeneratorTest` checks payment ID construction.
- `CustomerGraphGeneratorTest` checks generated order-item and payment IDs within a customer graph.

A full SBT test run was reported as passing on 2026-10-25:

- Tests run: 100
- Suites completed: 29
- Failures: 0
- Aborted suites: 0

The exact Git revision tested was not recorded in the supplied test output. These figures are historical verification evidence and must not be attributed to a particular commit without confirming the tested revision.

The passing test suite provides evidence for the behaviors covered by its tests. It does not, by itself, establish that every dataset-wide verification item below has been satisfied.

Further verification should confirm:

- Uniqueness of complete identifiers across a full generated dataset.
- Referential consistency between embedded parent IDs and explicit relationship fields.
- Correct counter reset behavior at customer and order boundaries.
- Reproducibility under the configured parallel-generation strategy.
- Correct behavior at sequence values exceeding six digits, if such values are supported by the intended workload.

## Related documentation

- [Deterministic parallel generation (ADR-0003)](ADR-0003-deterministic-parallel-generation.md)
- [Architecture](../ARCHITECTURE.md)
- [Data model](../DATA_MODEL.md)
- [Project state](../PROJECT_STATE.md)