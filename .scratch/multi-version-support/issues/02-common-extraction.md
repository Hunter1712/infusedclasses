# 02: Extract version-agnostic decisions into common module

**What to build:** Move the version-agnostic decision classes and their tests out of the version modules into `common`, and wire each version module to depend on `common`. After this, gameplay logic has a single source of truth while version-coupled adapters remain per module.

**Blocked by:** 01 (Multi-module build scaffolding)

**Status:** done

- [x] All classes with no Minecraft imports live in `common`
- [x] Their transitive pure dependencies are included in `common`
- [x] Each version module depends on `common` and no longer duplicates those files
- [x] `common` tests pass
- [x] All three version modules still compile after extraction
- [x] No gameplay decision logic is duplicated across modules

## Resolved 2026-10-31

The value-type edge was lifted by introducing four `common`-side types, and the decision
core moved with its tests:

- `ModIds` — the namespace and the one rejection line
- `AttributeRef` — the attribute a modifier lands on, as a registry id string
- `LineStyle` — the chat style a decision names
- `StatusView` — the seam a status rider reads a snapshot through

Moved to `common` and deleted from all three version modules (byte-identical before the
move): `ChatLine`, `ClassDefinition`, `ClassRegistry`, `PassiveEffect`, `HpScaledPassive`,
`ConditionalPassive`, `Momentum`, `Burden`, all six ability modules, `AbilityOutcome`,
`AbilityRoster`, `BindMorph`, `ClassPresentation`, `ClassInfo`, `WindowFamily`, `CritRule`,
`CritRoll`, `PassiveStacks`, `PlayerRuntime`, `PlayerRuntimes`, `MeleePlan`, `PlayerClasses`,
`CardLayout` and `CardWrap`, plus their tests; `PassiveSweep` holds the switch sweep that was
triplicated inside `PassiveApplier`.

**Remaining boundary, documented rather than forced:** `StatusHud` and `StatusStore` consume
the status snapshot, which owns its own wire codec and therefore stays per version. They are
coupled to a per-module type, not to a game object. ADR-0033 point 2 records this.

Result: 41 main and 23 test classes in `common`, 30-31 adapter files per version module, no
Minecraft or Fabric import anywhere in `common`, and zero duplicated decision files.
