# Documentation Index

## Start here
1. Read [Project State](PROJECT_STATE.md) for the current implementation and next steps.
2. Read [Decision Records](decisions/README.md) before changing established behavior.
3. Follow the repository instructions in [`AGENTS.md`](../AGENTS.md).

## Documents
- `PROJECT_STATE.md` — verified project snapshot, completed milestones, planned Phase 2 deliverables, acceptance criteria, gaps, and next steps.
- `decisions/README.md` — how decisions are proposed, accepted, reviewed, and superseded.
- `decisions/ADR-0001-identifier-conventions.md` — accepted entity identifier contract.
- `decisions/ADR-0002-build-system-branch-strategy.md` — SBT and Gradle branch responsibilities.
- `decisions/ADR-0003-deterministic-parallel-generation.md` — accepted chunk-based reproducibility design.

## Maintenance
Update project state when a meaningful feature, interface, architecture, test result, or known limitation changes.
Create an ADR when a decision affects long-term behavior, compatibility, architecture, data contracts, or build strategy.

Keep documentation concise and evidence-based. Do not copy entire source files into documentation.

## Architecture documentation

- [Architecture](ARCHITECTURE.md) — component responsibilities, current entry-point flow, and graph-generation boundaries.

## Data model

- [Data model](DATA_MODEL.md) — entity schemas, relationships, identifiers, and generation cardinalities.
