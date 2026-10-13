# ADR-0001: Entity Identifier Conventions

- Status: Accepted
- Date: 2026-10-14

## Context
Generated entity IDs must be deterministic, unambiguous, and consistent with their parent-child relationships. The previous implementation used inconsistent underscore-separated formats and did not embed parent IDs in all child IDs.

## Decision

Use these canonical formats:

| Entity | Example | Sequence scope |
|---|---|---|
| Customer | `CUST000001` | Global ordinal supplied by caller |
| Order | `ORD000001_CUST000001` | Per customer |
| Address | `ADDR000001_CUST000001` | Per customer |
| Order item | `ITEM000001_ORD000001_CUST000001` | Per order |
| Payment | `PAY000001_ORD000001_CUST000001` | Per order |

### Formatting rules
- Entity prefixes are immediately followed by six-digit, zero-padded sequence numbers.
- Underscores separate hierarchical components.
- Child identifiers embed the relevant parent identifier.
- Sequence counters are scoped as shown in the table.
- Customer ordinals are supplied by the caller; this ADR does not define how a global ordinal is allocated.
- Explicit relationship fields remain mandatory; embedding a parent ID does not replace referential integrity.

## Consequences
- Generator implementations and tests must use these formats.
- Generator instances must have the intended lifecycle so counters reset at the correct parent boundary.
- Existing generated datasets using legacy formats may require migration or regeneration.
- Parallel generation must allocate ordinals and counters deterministically if reproducible output is required.

## Alternatives considered
1. Keep the legacy formats: rejected because they do not meet the canonical hierarchical convention.
2. Use globally increasing child sequences: rejected in favor of per-parent sequences.
3. Embed parent IDs and retain explicit relationship fields: accepted.

## Verification
After implementation, verify:
- Exact ID strings for each entity.
- Counter reset behavior at each parent boundary.
- Uniqueness of complete IDs within the generated dataset.
- Referential consistency between embedded IDs and explicit relationship fields.
- Reproducibility for a fixed seed and fixed generation order.

Implementation and test evidence will be recorded after the changes are made and verified.
