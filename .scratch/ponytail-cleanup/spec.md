# Spec: Ponytail Cleanup (Class System)

Status: done

## Problem Statement

The class system works and is already lean, but the ponytail audit found dead APIs, two parallel serialization paths for the class id, a few duplicated lookups, one null-safety gap in class id parsing, an unused attribute branch, and stale documentation. Each item is small on its own, but together they raise the cost of adding the next class. A maintainer trying to add Ranger or Mage today must guess which registry lookup, which serialization path, and which cooldown query is canonical.

## Solution

Remove the dead code, unify on one canonical path per concern, fix the small robustness and duplication issues with one-line changes, and correct the stale documentation. No player-visible behavior changes. Warrior stats, War Cry effect and cooldown, first-join picker flow, persistence across relogs, and the admin command all behave exactly as before.

## User Stories

1. As a maintainer, I want only one class lookup API on the registry, so that I never guess between two.
2. As a maintainer, I want only one class id serialization path, so that picker, persistence, and tests agree.
3. As a maintainer, I want only one canonical cooldown query in production, so that War Cry logic has one source of truth.
4. As a maintainer, I want the modifier-removal helper scoped to its owning module, so that no external caller depends on internals.
5. As a player, I want class selection and War Cry timing to behave identically after cleanup, so that I notice nothing.
6. As a player picking a class, I want an invalid or locked id to be rejected exactly once, so that retry behavior is unchanged.
7. As a player rejoining, I want my class re-applied silently without the picker, so that first-join detection is unchanged.
8. As a server admin, I want the class set command to validate and apply in one place, so that unknown or locked classes still fail cleanly.
9. As a Warrior, I want War Cry to fire only when off cooldown and otherwise show remaining seconds, so that cooldown feedback is unchanged.
10. As a maintainer, I want malformed class ids to yield no class instead of risking a null-map lookup, so that bad NBT and bad command input are safe.
11. As a maintainer, I want the attribute lookup to cover only attributes a class actually uses, so that future classes add branches when needed.
12. As a maintainer, I want the picker screen to compute its layout count once, so that per-frame work stays minimal.
13. As a maintainer, I want the mod description to match actual Warrior behavior, so that players are not promised a melee bonus that does not exist.
14. As a maintainer, I want the documented Java toolchain to match the build, so that a fresh checkout builds without guessing.
15. As a maintainer, I want the documented modifier mechanism to match the implementation, so that the next class copies a working pattern.

## Implementation Decisions

- The class registry exposes a single nullable lookup as the canonical query. The unused Optional-returning lookup is removed.
- Class id persistence uses the string read/write pair owned by the per-player class state as the single canonical path. The record-level NBT helpers on the class definition are removed, since production persistence goes through the player save-data hooks and never touches them.
- Cooldown tracking exposes fire plus remaining-time as the canonical production queries. The separate ready boolean is removed from production; callers treat zero remaining as ready.
- The modifier-removal helper becomes an internal detail of the per-player class state module, since its only callers are class set and class refresh inside that same module.
- Wall-clock access in the server initializer is inlined at the two call sites inside the War Cry flow. No dedicated clock helper remains, because the cooldown type already accepts the current time as a parameter and is directly assertable.
- The join flow resolves the per-player state once into a local and reuses it for refresh plus first-join detection, instead of resolving twice.
- The class-select flow validates once in the per-player state module. The networking handler no longer performs a redundant pre-lookup before delegating.
- Class id parsing guards malformed input before touching the registry map, so unknown, missing, and unparseable ids all yield no class through one path.
- The vanilla attribute lookup covers only attributes with at least one passive in the registry. The unused attack-damage branch is removed and returns with the next class that needs it.
- The class picker computes the class count once per layout and render pass instead of querying the registry size repeatedly.
- Mod metadata wording is corrected to match implemented Warrior behavior: increased health, armor, and toughness, reduced speed, War Cry as the damage tool, no flat melee bonus.
- Build and spec agree on the Java toolchain version; one of the two is updated to match the other.
- The spec wording for the modifier mechanism is corrected from UUID namespacing to identifier namespacing to match the current platform API.
- Cooldown state is intentionally not persisted across relogs. A fresh login starts off cooldown. This is accepted as the lazy default; persistence is added only if the reset becomes an exploit. The ceiling is recorded with a brief code comment naming the upgrade path.

## Testing Decisions

- A good test asserts external behavior of the class-logic module: given a class id, the produced modifiers match the design numbers; the cooldown state machine rejects a second use inside the window and accepts one at its edge; an unknown or malformed id yields no class; switching class removes prior modifiers.
- Tested modules are the class registry and definitions, the cooldown state machine, and the per-player class id round-trip through strings. Existing plain JUnit suites for these modules are updated: assertions covering removed APIs are deleted, behavior assertions for the canonical paths remain and keep passing.
- Prior art is the existing plain JUnit suites with no game bootstrap and no gametest harness. That style is kept: no new harness, no rendering, GUI layout, keybind plumbing, or mixin-internal tests. Those paths are out of scope for automated testing here.
- The server initializer join, command, and War Cry wiring has no unit seam and is not given one by this cleanup. Correctness for those paths rests on unchanged behavior plus the pure-logic suites above, per the selected seam.

## Out of Scope

- Merging the four networking payload types into fewer files or deduplicating the payload type constants; the current shape follows the platform idiom and stays.
- Persisting cooldowns across relogs beyond documenting the accepted reset.
- New classes beyond Warrior, War Cry radius or team behavior, new abilities, class kits or items, GUI art or assets, config files, datapack extensibility.
- Balance tuning of Warrior numbers.
- A new test harness, gametest setup, or coverage for rendering, GUI, keybinds, or mixins.

## Further Notes

- Source for this spec is the ponytail audit of the current codebase plus the class-system spec's Testing Decisions. The audit's per-item code pointers were used to derive the decisions above and intentionally do not appear here, since file paths age quickly.
- The two pre-existing simplification notes in the server initializer (plain string command argument, flat particle ring for War Cry) remain valid and are untouched by this cleanup.
- The class-system spec contains a contradiction between the early melee-bonus user story and the later no-flat-damage decision plus its regression test. This cleanup sides with the implemented and tested behavior (no flat bonus, War Cry as the damage tool) and fixes the wording to match.
