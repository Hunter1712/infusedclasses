# 02: Extract version-agnostic decisions into common module

**What to build:** Move the version-agnostic decision classes and their tests out of the version modules into `common`, and wire each version module to depend on `common`. After this, gameplay logic has a single source of truth while version-coupled adapters remain per module.

**Blocked by:** 01 (Multi-module build scaffolding)

**Status:** ready-for-agent

- [x] All classes with no Minecraft imports live in `common`
- [x] Their transitive pure dependencies are included in `common`
- [x] Each version module depends on `common` and no longer duplicates those files
- [x] `common` tests pass
- [x] All three version modules still compile after extraction
- [x] No gameplay decision logic is duplicated across modules

## Reopened 2026-10-31 (spec review)

The mechanical rule extracted the seven leaf decisions only. The decision core is still
triplicated, byte-identical across all three version modules, because each is transitively
tangled with Minecraft *value types* (not game objects):

- `ClassDefinition.id` is a `net.minecraft.resources.Identifier`
- `PassiveEffect.Modifier` carries `ResourceKey<Attribute>` and an `Identifier` modifier id
- every ability module names its own modifier ids, and `ChatLine.style` is `ChatFormatting`

No `net.minecraft` import blocks the move of `MeleePlan`, `PlayerRuntime`, `PlayerRuntimes`,
`ConditionalPassive`, `PassiveStacks`, `CritRule`, `CritRoll`, `BindMorph` and `AbilityOutcome`
directly — the coupling is the value types named above. Their tests (`MeleePlanTest`,
`CritRollTest`, `LegionaryTest`, `PlayerRuntimeDeepeningTest`, `PlayerStatusTest`, ...) would
move with them.

**Remaining work, in order:**
1. Replace `ChatLine.style`'s `ChatFormatting` with a `common`-side style name (or null); the
   adapter maps it to `ChatFormatting` when it builds the `Component`.
2. Replace `PassiveEffect.Modifier`'s `ResourceKey<Attribute>` + `Identifier` with a
   `common`-side pair (attribute registry name, modifier id string); `AttributeWriter` resolves
   the `ResourceKey` at its single use site.
3. Replace `ClassDefinition.id` (and the roster's ability ids) with a plain string, and have the
   adapters build `Identifier`s from it at their use sites.
4. Move the classes above plus their tests to `common`; delete the three copies; keep the pure
   tests running once.
5. Re-run `./gradlew build` and all four modules' tests.

ADR-0033 point 2 records where the rule stops and why. Ticket closes when the checklists' first,
second and last lines are literally true.
