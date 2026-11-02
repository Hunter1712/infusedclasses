# Spec: Audit fixes 03

Status: implemented — in-game picker check owed

## Problem Statement

The mod's rules are in good shape, but the repo is harder to inherit than it should be. The two version modules carry the same adapter and test files twice, so a fix on one Minecraft version can miss the other and both suites stay green. The one piece of damage arithmetic the build cannot see — the last-hurt memory around a settle bonus — has no test. The docs still describe the HP-scaled buckets and the 1.20.1 module that were deleted, and the README's client-deploy command does not exist. A new contributor is taught a model the code no longer has, and a maintainer pays for every port twice.

## Solution

Move the byte-identical adapter and test sources into one shared source directory compiled by both version modules; leave only genuinely version-specific files in each module. Pin the last-hurt restore arithmetic and the passive sweep-set invariant with pure JUnit tests. Delete the dead duplicate constant and the test-only helper methods. Correct the docs, the README command, the classless-key javadoc and the picker guides. Cache the picker's measured card so it is not re-wrapped every frame.

No gameplay number, ability, class or balance rule changes. The work reuses the existing decide-then-apply seams; the only new production type is a small pure helper for the last-hurt restore.

## User Stories

1. As a maintainer, I want each byte-identical adapter to exist once, so that a fix on 1.21.1 also lands on 26.3.
2. As a maintainer, I want the shared adapters compiled per version module, so that each jar still targets its own Minecraft version.
3. As a maintainer, I want a genuinely version-specific adapter to stay in its module, so that sharing does not force a fake abstraction.
4. As a maintainer, I want the existing identical tests to exist once, so that a test fix is not a two-place edit.
5. As a maintainer, I want the full `./gradlew test` to compile and run the shared sources in both modules, so that the move is verified by the build.
6. As a player, I want a crit bonus smaller than the base hit to keep landing, so that the damage matches the card.
7. As a player, I want a bonus that is fully eaten to leave the last-hurt memory unchanged, so that the next hit is not double-counted.
8. As a maintainer, I want the last-hurt restore arithmetic covered by a pure test, so that a typo fails the suite instead of changing damage silently.
9. As a maintainer, I want the sweep set to be pinned for every registered passive, so that a tier change cannot leave a permanent attribute on the player.
10. As a Juggernaut, I want a bar that falls below a checkpoint to lose that checkpoint's modifier, so that the bar and the stats agree.
11. As a future kit author, I want the sweep-set rule to fail a test when a new tier drops an id, so that I fix the applier before shipping.
12. As a new contributor, I want the glossary to describe the passive effect and applier that exist, so that I do not look for a deleted bucket.
13. As a new contributor, I want the card comment to describe the lines it draws, so that I do not add a family that has no code.
14. As a new contributor, I want ADR-0033 to name the modules that exist, so that the architecture record matches the build.
15. As a new developer, I want the README's client-deploy command to work, so that my first client test does not fail on a missing task.
16. As a contributor, I want the classless-key javadoc to match the silent-reject rule, so that I do not reopen a press ADR-0005 closed.
17. As a player, I want the guides to say the picker reopens on join while I have no class, so that I know I can relog instead of asking an admin.
18. As a support person, I want the FAQ to give the real recovery step, so that I do not send a player to an admin for a relog fix.
19. As a maintainer, I want the dead duplicate constant gone, so that the next edit cannot pick the wrong copy.
20. As a test reader, I want `ProtocolTest`'s name to match its five channels, so that I do not miss one when adding a sixth.
21. As a maintainer, I want the test-only helper methods gone, so that the decision modules expose only what production uses.
22. As a client player, I want the picker to stop re-wrapping the card every frame, so that the screen does not do the same work 60 times a second.
23. As a client player, I want the picker to still update when I change page or hover, so that the cache does not freeze the card.
24. As a maintainer, I want the audit's 11 findings closed in one round, so that the repo is left cleaner than it was found.
25. As a maintainer, I want the round verified by the JUnit suite, the doc/arch flags and the client deploy, so that no fix is declared done on compile alone.

## Implementation Decisions

- **Shared adapter sources.** Add one shared source directory for the byte-identical adapters and tests, and include it in both version modules' main and test source sets. Move the identical files there once; delete the duplicate copies. A file that diverges later moves back to its module. The shared directory is not a new Gradle module and not part of `common`: it has Minecraft imports, so it compiles inside each Fabric module with that module's mappings.
- **Last-hurt restore.** Extract the pure rule "the memory after a settled bonus is the old memory plus what the bonus actually took" into a small `common` helper that takes the old memory, the health before the bonus and the health after it. The adapter reads the memory, clears it, calls the vanilla hurt, and writes the helper's answer back. The helper clamps a negative delta to zero. The lethal `AFTER_DEATH` branch keeps its in-game check; it is event wiring, not arithmetic.
- **Sweep-set invariant.** Add one test over the registered classes: for every passive, every tier's row and the max-stack row must name the same modifier ids, and the class-switch sweep must include every id a lower tier could write. This pins the convention the applier already relies on. Delete `Momentum.allModifiers()`; the test builds the expected set from the passive's own rows.
- **Test-only helpers.** Delete `OffloadAbility.windowModifiers()` and `AequilibriumAbility.windowModifiers()`. The window-family tests assert the wanted rows' ids and amounts through the family, not through a second copy of the pair.
- **Dead constant.** Delete the package-private `UNKNOWN_OR_LOCKED` copy from each version entry point. Keep the live `ModIds` string; renaming its "or locked" wording is a product copy decision, not part of this round.
- **Picker cache.** Cache the resolved card view by page, hover state and screen size. `renderBackground` fills the cache; `render` and `mouseScrolled` read it. A page change, hover change or resize invalidates it. The cache holds the same `View` value the screen already builds; no new rendering path.
- **Docs.** Rewrite the glossary's passive-effect and passive-applier entries, the class entry, the Legionary entry and the card entry to remove HP-scaled buckets. Remove the same clauses from the adapter comments. Update ADR-0033's module list and add a short note that 1.20.1 was removed by ADR-0034 and that identical adapters now share a source directory. Update the ADR index summary. Fix the README's deploy command to name the version module. Rewrite the classless-key sentence in `BindMorph` to match ADR-0005. Change the picker guides from "once, on first join" to "on every join until a class is chosen".
- **No new ADR.** The shared source directory is a source-layout detail inside ADR-0033's "adapters per version" rule; a note on that ADR is enough. The other changes restore documented behavior or delete dead text.
- **No gameplay change.** No class numbers, cooldowns, multipliers, stack caps or ability gates change. The last-hurt helper must return exactly the value the adapter computes today.

## Testing Decisions

A good test asserts the player-visible rule or the pure decision, not the wiring. It constructs no `ServerPlayer`, level or screen, and it follows the repo's existing plain-JUnit style.

- **Shared sources:** no new test. The full `./gradlew test` is the check: both modules must compile the shared directory and run the moved tests. Prior art: the existing multi-module suite.
- **Last-hurt restore:** a new pure test in `common` covers a landed bonus (old memory plus the health delta), a fully eaten bonus (old memory unchanged), a rejected/zero delta (unchanged) and a negative delta (clamped to zero). Prior art: `DamageSettleTest` for `settleMath`, `ConditionalPassiveStacksTest` for pure time math.
- **Sweep set:** a new pure test in `common` walks every registered class and passive, asserts each tier's id set equals the max-stack id set, and asserts `PassiveSweep.sweepFor` removes every id the old class's lower tiers can write. Prior art: `ClassSwitchCleanupTest`, `MomentumTest`.
- **Window helpers:** update `WindowFamilyTest` to assert the wanted rows' ids and amounts directly; no production helper is needed for the test.
- **Picker cache:** no JUnit. The in-game check opens the picker, changes pages, hovers and resizes, and confirms the card still redraws; the client deploy and `latest.log` read are the AGENTS.md checks.
- **Docs:** `tools/doc-flags.sh` flags a changed class with an untouched glossary; the round runs it and the arch flag before it is called done. The README command is checked by running `./gradlew tasks --all` and seeing the per-module tasks.
- **Round check:** `./gradlew test` green on both modules; `tools/doc-flags.sh` and `tools/arch-flags.sh` read; client-visible picker change shipped through the per-module `deployClient`; `latest.log` grepped for `infusedclasses` errors.

## Out of Scope

- Renaming the "Unknown or locked class" copy. No lock exists, but the wording is a product decision; the dead duplicate constant is the only code change.
- Deleting the HP-scaled machinery. It is already gone; this round only removes the stale prose.
- A Fabric gametest harness or automated client rendering tests.
- Changing any class number, ability gate, cooldown, multiplier or stack cap.
- Changing `/class pick` from gamemaster-only. The picker reopens on join, so a player has a recovery path without a command change.
- The version-specific adapter files that differ between 1.21.1 and 26.3; they stay in their modules.
- New classes, abilities or ADRs.

## Further Notes

- Audit mapping: findings 1–11 are all in this spec. Finding 1 is the shared source move; finding 2 the last-hurt test; finding 3 the sweep test; finding 4 the stale docs; finding 5 the README command; finding 6 the `BindMorph` javadoc; finding 7 the picker guides; finding 8 the dead constant; finding 9 the test-only helpers; finding 10 the `ProtocolTest` name; finding 11 the picker cache.
- The shared move is a `git mv` of byte-identical files. If a shared file later needs a version-specific API, move it back to the module rather than adding a branch.
- The last-hurt helper is the only new production type. It must not change the adapter's current answer: old memory plus `max(0, before - after)`.
- In-game checks: open the picker, change pages, hover, resize; confirm the card and the HUD still draw. The AGENTS.md melee hand checks are unchanged, but the settle helper touches the guarded-hurt path, so re-check a bonus smaller than the base hit and a shield-eaten hit.
- `ProtocolTest` stays per module because the two copies differ on the identifier type
  (`ResourceLocation` vs `Identifier`); the rename is one line in each copy.
- Commit the round on the current branch.
