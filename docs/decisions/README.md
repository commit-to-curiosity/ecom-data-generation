# Architecture Decision Records

Architecture Decision Records (ADRs) preserve decisions that should survive code changes, branch changes, and context loss.

## Statuses
- **Proposed** — suggested but not approved.
- **Accepted** — explicitly approved and currently authoritative.
- **Under review** — conflicting evidence, an unresolved question, or confirmation is required.
- **Superseded** — replaced by a later decision; retain the historical record and link the replacement.

## Decision records

- [ADR-0001: Entity Identifier Conventions](ADR-0001-identifier-conventions.md) — Accepted.
- [ADR-0002: SBT and Gradle Branch Strategy](ADR-0002-build-system-branch-strategy.md) — Proposed.
- [ADR-0003: Deterministic Parallel Generation](ADR-0003-deterministic-parallel-generation.md) — Accepted.

## When to create an ADR
Create one for decisions affecting:
- Public or persisted data formats and identifier contracts
- Architecture, component boundaries, or entity relationships
- Reproducibility and parallel execution
- Configuration formats or compatibility
- Build systems and branch strategy
- Significant performance/reliability trade-offs

## Required sections
Each ADR should contain:
1. Title and status
2. Date
3. Context
4. Decision
5. Consequences
6. Alternatives considered
7. Evidence or related implementation/tests
8. Supersedes/superseded-by links, where applicable

## Rules
- An ADR is not accepted merely because code implements the behavior.
- Link implementation and tests when known.
- Keep unresolved questions visible.
- Do not rewrite historical decisions as though they were always current.
- When a decision changes, create a new ADR and mark the old one Superseded.
- Use the project state document for current implementation status; use ADRs for why a choice was made.
