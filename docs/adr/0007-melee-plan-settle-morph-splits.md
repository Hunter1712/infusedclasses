# ADR-0007: Melee plan, damage settle, bind morph, and adapter splits

Status: accepted

## Context

The Legionary kit (ADR-0006) re-added to the server entry what ADR-0005 had removed:
ability-id comparisons gating rewards (`contains(ROTATE_ABILITY)` / `contains(IMPETUS_ABILITY)`
in the melee hook, plus a `takeFor` helper covering two branches), a second
re-entrancy flag next to the entry's (`resolvingEmpower` beside `resolvingBonus`), and
a second job inside `RendApplier` (stance settle under Rend's name). `PassiveApplier`
(219 lines) held tick, lifecycle, spender economics, stance auras, and attribute
mechanics in one file, and `PlayerRuntime` (233 lines) held four timestamp fields with
one shared `isLive` check. The picker resolved each card twice per frame through two
paths (`cardFor` for button/tooltip, parts for frame/body).

## Decision

1. **The entry never compares ability ids, anywhere.** `MeleePlan` decides what one
   melee hit earns and settles (stack gate, deal/take mults, Impetus bonus); the entry
   maps, `DamageSettle` applies. This revisits ADR-0005's bind-morph carve-out:
   `BindMorph` decides what a reused bind means (`R` rotate-or-Rush, `G`
   Rend-or-Impetus-or-Testudo), and the receivers wire only.
2. **One guarded hurt.** `DamageSettle` owns the single `hurtGuarded` plus the
   stance-mult settle both branches funnel through; `isResolving` is the one flag the
   hook checks. `RendApplier` keeps press announces only.
3. **Adapters split along their jobs.** Stance auras move to `StanceApplier`,
   attribute write/remove mechanics to `AttributeWriter`; `PassiveApplier` keeps tick,
   switch, and spend orchestration. This is the split ADR-0005 named as next.
4. **Windows are one type.** The four timestamp fields become `Window` (arm/isLive/clear);
   verbs and lanes are unchanged.
5. **One card seam.** The screen resolves one `CardModel` per card per frame and passes
   it; the renderer draws the model's lines. Parts stay as the pinned decision seams.
6. **One trigger scan, one name.** `RendAbility.momentumOf` and
   `PassiveApplier.pricedPassive` inline to `ClassDefinition.passiveForTrigger`.

## Consequences

- A third melee class adds registry entries plus plan/morph rows; the entry, the
  applier, and the receivers stay untouched.
- The settle, guard, writer, and stance seams are adapter-only (need a player), so
  they carry no JUnit; the plan and morph seams are pure and pinned (`MeleePlanTest`,
  `BindMorphTest`).
- Cost: four more modules to know about (`MeleePlan`, `DamageSettle`, `BindMorph`,
  `StanceApplier`, `AttributeWriter`). Each passes the deletion test alone; none
  introduces a seam with a single hypothetical adapter — settle has three callers,
  the writer four, the plan two branches.
