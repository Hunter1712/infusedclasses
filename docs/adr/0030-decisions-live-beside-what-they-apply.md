# ADR-0030: Each decision lives beside what it applies to

Status: accepted

## Context

The review's second candidate found the applier deciding for everyone: `PassiveApplier` held the
stance, wheel and Offload `wanted` decisions plus the family table, and a
`Momentum.PASSIVE.equals(passive)` compare inside its tick loop, so a second bar or window family
edited the adapter that applies modifiers. The bar's rows lived in `Momentum` while the
`ConditionalPassive` row itself never said it writes rows. The same shape as ADR-0029 one layer
down: constants in one module, the decision about them in an adapter.

## Decision

1. **Window families live with their ability.** `WindowFamily` becomes a top-level record
   (marker, full sweep set, `wanted(state)`) and each window ability exposes `windowFamily()`
   beside the modifiers it spells: Rotate (both stances), Aequilibrium (the oath window's plate),
   Roulette (the live spin), Offload (the loosened pair). `PassiveApplier` keeps only the list of
   rows and one reconcile loop that walks it exactly as before.
2. **Bars own their rows.** `ConditionalPassive` gains `barRows` (a stack count to modifier rows
   function; empty by default). `Momentum.PASSIVE` passes `Momentum::forStacks`; the tick
   reconciles every passive through `barRows` and the identity compare dies, so a second bar adds
   a passive with its own rows instead of a branch.
3. **Aequilibrium's oath window is its own family.** It is marked under
   `AEQUILIBRIUM_ABILITY`, not folded into the stance family, so a class carrying Aequilibrium
   without Rotate also gets and sweeps its plate. No registered class changes; only definitions
   the registry does not carry.

## Consequences

- Adding a window ability or a bar does not edit `PassiveApplier`; the applier is two loops.
- The switch cleanup reads the same two sources the tick writes from — `barRows` at the cap and
  each held family's full set — so `cleanupFor` and the tick cannot drift.
- `WindowFamilyTest` re-points at the ability rows, `MomentumTest` pins the bar's own rows, and
  `ClassSwitchCleanupTest` pins the Aequilibrium-without-Rotate sweep.
