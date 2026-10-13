# Repository Instructions

## Project
ShopSphere E-Commerce Data Generator.
Organization: `com.shopsphere`.
Scala version: 2.13.18.
Current primary development branch: `main` (SBT).
Gradle build branch: `main-gradle`.

## Before changing code
1. Read `docs/PROJECT_STATE.md`.
2. Read `docs/decisions/README.md`.
3. Read applicable decision records under `docs/decisions/`.
4. Inspect the relevant implementation and tests before proposing changes.
5. Check the current Git branch and working tree.

## Engineering rules
- Keep Scala on 2.13 unless an accepted decision changes the version.
- Preserve seeded reproducibility; document any change to random-number consumption.
- Do not assume sequential reproducibility implies parallel reproducibility.
- Preserve entity relationships and validate foreign-key-like references.
- Do not silently change identifier formats, configuration contracts, or generation semantics.
- Prefer focused changes with tests covering the behavior being changed.
- Do not claim tests pass unless they were actually executed; report the command and result.
- Distinguish implemented, tested, and wired into the application entry point.
- Avoid unrelated formatting or build-system changes in application commits.

## Documentation rules
- Record durable decisions in an ADR before or alongside implementation.
- Use explicit decision statuses: Proposed, Accepted, Under review, Superseded.
- Update `docs/PROJECT_STATE.md` when architecture, verified behavior, known gaps, or next steps change.
- Record evidence and verification commands for important claims.
- Never resolve conflicting historical notes by guessing. Mark the conflict and request confirmation.
- When superseding a decision, link the replacement ADR.

## Build-system branches
- `main` is the SBT development branch.
- `main-gradle` is the Gradle build branch.
- Do not merge build-specific files blindly between these branches.
- Review mixed application/build commits carefully; cherry-pick application-only commits when appropriate.
- Verify source and test parity before claiming the branches are aligned.
