# Documentation Index

## Start here

1. Read [Project State](PROJECT_STATE.md) for current implementation status, completed milestones, known gaps, and next steps.
2. Read [Architecture](ARCHITECTURE.md) to understand component responsibilities, generation flow, and output handling.
3. Read [Data Model](DATA_MODEL.md) for entity schemas, relationships, identifiers, and generation cardinalities.
4. Read [Decision Records](decisions/README.md) before changing established behavior.
5. Follow the repository instructions in [`AGENTS.md`](../AGENTS.md).

## Documents

- `PROJECT_STATE.md` — current project snapshot, implemented capabilities, milestone status, known limitations, and roadmap.
- `ARCHITECTURE.md` — component responsibilities, application flow, scenario-based generation, chunk processing, and output publication.
- `DATA_MODEL.md` — entity schemas, relationships, identifiers, and graph-generation behavior.
- `decisions/README.md` — how decisions are proposed, accepted, reviewed, and superseded.
- `decisions/ADR-0001-identifier-conventions.md` — accepted entity identifier contract.
- `decisions/ADR-0002-build-system-branch-strategy.md` — SBT and Gradle branch responsibilities.
- `decisions/ADR-0003-deterministic-parallel-generation.md` — accepted design for deterministic chunk-based generation and reproducibility.

## Maintenance

Update project state when a meaningful feature, interface, architecture, test result, or known limitation changes.

Update architecture and data-model documentation when their corresponding implementation changes.

Create an ADR when a decision affects long-term behavior, compatibility, architecture, data contracts, or build strategy.

Keep documentation concise and evidence-based. Do not copy entire source files into documentation. Distinguish intended design, implemented behavior, tested behavior, and verified guarantees.