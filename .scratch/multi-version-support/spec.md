# Spec: Multi-version support (1.20.1, 1.21.1, 26.3; drop 1.21.11)

Status: done

## Problem Statement

The mod currently targets a single Minecraft version (1.21.11). Players on 1.20.1, 1.21.1 or the 26.x line cannot install it, and the project metadata, docs and build all say "1.21.11 only". The user wants the mod supported on 1.20.1, 1.21.1 and 26.3, with 1.21.11 removed from support. Because a single Fabric jar can only target one Minecraft version, this requires a multi-version build rather than a wider version range.

## Solution

Restructure the project into a multi-module Gradle build: a shared `common` module holding the version-agnostic decisions and their tests, plus one Fabric module per target Minecraft version (1.20.1, 1.21.1, 26.3) holding the version-coupled adapters, mixins, client code, networking and resources. Each version module produces its own jar named `infusedclasses-<mod>+<mc>.jar` with metadata declaring `~<mc>`. Remove 1.21.11 from properties, metadata, docs and tooling defaults.

## User Stories

1. As a player on Minecraft 1.20.1, I want to install the mod, so that I can use the classes on my version.
2. As a player on Minecraft 1.21.1, I want to install the mod, so that I can use the classes on my version.
3. As a player on Minecraft 26.3, I want to install the mod, so that I can use the classes on my version.
4. As a player on Minecraft 1.21.11, I want clear messaging that the version is no longer supported, so that I know to move to a supported version.
5. As a modpack author, I want one jar per game version with a matching `+<mc>` suffix, so that I can pick the correct artifact.
6. As a server operator, I want the same jar on client and server for each version, so that the mod stays server-authoritative everywhere.
7. As a developer, I want pure decisions and tests in one shared module, so that gameplay logic is not duplicated across versions.
8. As a developer, I want version-coupled code isolated per module, so that API and mixin differences do not leak into shared logic.
9. As a developer, I want each version module to compile and test independently, so that a breakage in one version does not block the others.
10. As a developer, I want in-game validation on each supported version, so that mixin and client regressions are caught per version.
11. As a maintainer, I want docs and metadata to list only supported versions, so that support expectations stay accurate.
12. As a QA tester, I want the existing pure tests to keep passing unchanged, so that the refactor does not alter gameplay decisions.
13. As a QA tester, I want per-version smoke builds, so that adapter code compiles against each target's mappings.
14. As a player, I want identical gameplay across versions, so that class balance and behavior do not differ by Minecraft release.
15. As a player, I want the status HUD, picker, commands and cooldowns to work on every supported version, so that no feature is version-locked.
16. As a player, I want class data to persist across relogs on every supported version, so that my choice survives.
17. As a player, I want crits, Momentum, stances, Roulette and All In to behave identically on every supported version.
18. As a player, I want shield-eaten hits to earn nothing while Rush, Impetus and Rend stay pending on every version.
19. As a developer, I want mixin policy unchanged per version, so that only targets and signatures adapt.
20. As a developer, I want the build tooling to default to a supported version, so that local development does not reference a removed version.

## Implementation Decisions

- The build becomes multi-module: a plain Java library `common` module and one Fabric module per target version (1.20.1, 1.21.1, 26.3). The Loom plugin applies only to version modules.
- `common` compiles to Java 17 bytecode so all three version modules can consume it; version modules may target their own runtime Java (17, 21 or 25) for adapters.
- Version modules depend on `common` for pure decisions and tests; they own Fabric entrypoints, mixins, networking codecs, damage hooks, client screens/HUD, attribute writes, particles/sounds and resources.
- The initial migration copies the current source tree into each version module so every module compiles before any shared extraction; extraction of the version-agnostic files into `common` follows once builds are green.
- Version-specific API differences are handled inside each version module: identifier type naming, save-data format, mixin targets/signatures, damage and hurt APIs, client screen and widget signatures.
- Mixin intent and policy stay identical across versions; only annotation targets and method signatures adapt.
- Each jar declares a single-version range (`~<mc>`) and carries the game version in its filename.
- Metadata, docs, README, project description, AGENTS and changelog are updated to list 1.20.1, 1.21.1 and 26.3 and to remove 1.21.11.
- Tooling defaults (client instance path, mixin mapping resolver, deploy task) move off the removed version to a supported default.
- An ADR records the multi-module version strategy as a hard-to-reverse architectural decision.
- The seam for testing is the module boundary: pure decisions are tested once in `common`, adapter compilation and smoke builds are tested per version module, and in-game checks remain manual per version.

## Testing Decisions

- Good tests assert external behavior of pure decisions with boundary values, not implementation details; prior art is the existing ability, runtime, card and HUD tests.
- `common` tests run once and must pass unchanged in spirit; any test that imports a version-coupled type moves to the relevant version module.
- Each version module must at least compile its main and test sources; full unit parity is the goal.
- Manual in-game verification on one instance per version covers mixin and client behavior: picker, Momentum, crit cue, shield-eaten hits, settle health drop, HUD.
- Out-of-scope for automated tests: rendering, corner mirroring, hide rules and emission timing.

## Out of Scope

- Supporting 1.21.11 after removal.
- A single jar covering multiple Minecraft versions.
- Rewriting gameplay rules or balance for any version.
- New features beyond version support.
- Automated in-game/UI tests.

## Further Notes

- 26.3 is the newest target and its API may shift; treat it as the riskiest port.
- Instance paths and Fabric API/loader versions for each target must be confirmed against the user's installed environments.
- Full parity is the stated goal; if a version lacks an API, shim it inside that version module rather than weakening shared behavior.
