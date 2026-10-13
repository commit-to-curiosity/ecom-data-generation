# ADR-0001: Entity Identifier Conventions

- Status: Under review
- Date: 2026-10-14

## Context
Identifier formats must be consistent, reproducible, and unambiguous across generated entities and their relationships.

A previously recorded intended convention is:

| Entity | Intended example |
|---|---|
| Customer | `CUST000001` |
| Order | `ORD000001_CUST000001` |
| Address | `ADDR000001_CUST000001` |
| Order item | `ITEM000001_ORD000001_CUST000001` |
| Payment | `PAY000001_ORD000001_CUST000001` |

The convention has no underscore between an entity prefix and its zero-padded sequence. Underscores separate hierarchical identifier components.

However, the implementation inspected during the October 2026 review uses different formats in several generators and tests. Examples observed include `CUST_000001`, `ADDR_000001`, and `PAY_000001`. The order generator also constructs its order ID from the customer ID and a sequence in a different order from the intended example.

These formats conflict and must not be silently reconciled.

## Decision
No canonical identifier format is approved by this ADR yet.

The project owner must confirm whether the intended examples above are authoritative or whether the current implementation is the desired contract.

Until confirmed:
- Do not mass-rename generated IDs.
- Do not update tests merely to make them agree with one assumed format.
- Inventory all entity ID generators, model references, configuration assumptions, and tests.
- Consider compatibility with existing generated datasets and downstream consumers.

## Consequences
A confirmed decision must specify:
- Prefix and zero-padding width for each entity.
- Whether child IDs embed parent IDs.
- Component ordering and separators.
- Whether sequence numbers are global, per generator, per customer, or per parent.
- Uniqueness scope and reproducibility requirements.
- Compatibility/migration policy for previously generated data.

## Alternatives considered
1. Adopt the previously recorded hierarchical examples.
2. Adopt the currently implemented format.
3. Use different formats by entity, only if explicitly justified and documented.

No alternative is selected yet.

## Evidence
Inspect the current implementations and tests for:
- `CustomerIdGenerator`
- `AddressGenerator`
- `OrderGenerator`
- `OrderItemIdGenerator`
- `PaymentIdGenerator`
- Corresponding generator tests

The exact repository state must be checked again before implementation changes.
