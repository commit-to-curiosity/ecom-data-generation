# ADR-0002: SBT and Gradle Branch Strategy

- Status: Proposed
- Date: 2026-10-14

## Context
The project maintains an SBT build and a separate Gradle migration. Build-specific files and settings differ, while application source, resources, and tests need to remain aligned.

At the baseline review:
- `main` is the SBT development branch.
- `main-gradle` is the Gradle build branch.
- A previous comparison found no differences under `src/` or `conf/` at the branch tips then inspected.

## Decision
1. Use `main` as the primary SBT development branch.
2. Keep `main-gradle` as the Gradle build branch unless the project owner changes this policy.
3. Do not merge build-specific files blindly between branches.
4. Prefer cherry-picking application-only commits to the other branch when needed.
5. Inspect mixed application/build commits before cherry-picking.
6. Resolve conflicts separately for build files and application files.
7. Verify source and test parity before asserting the branches are aligned.
8. Run the appropriate build and tests on each branch after relevant changes.

## Consequences
- Some application commits may need to be applied independently to both branches.
- Build behavior and test execution may differ between SBT and Gradle.
- Branch-specific verification results must be recorded separately.
- Shared application behavior should not depend accidentally on a build-specific setting.

## Alternatives considered
- Merge all changes between branches: rejected because it risks importing incompatible build configuration.
- Maintain only one build system: not selected; the existing migration is being preserved.

## Review trigger
Revisit this decision if Gradle becomes the primary build, SBT is retired, or the project adopts a single shared build configuration.
