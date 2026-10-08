# ADR-0008: Close the six architecture-review seams

Status: accepted

## Context

The architecture review (six candidates, 2026-10-08) found the seams from ADR-0005
and ADR-0007 leaking at their edges: the transient runtime handed out its mutable
cooldown lanes, the Rend adapter forwarded one call, the gate-and-arm shape was
copied across five abilities, the hit ordering lived in the untested entry, the card
renderer kept a definition path behind the model's back, and window resets enumerated
fields by hand.

## Decision

1. **Cooldown lanes stay the runtime's implementation.** `ROTATE_MS` moves to
   `Cooldowns`; ability triggers take remaining longs, never the lane object; the
   `cooldowns()` accessor is deleted.
2. **Rend lands through the shared announce path.** `RendApplier` (one forward) is
   deleted; only Rush keeps its own adapter, for the cleanse and Speed.
3. **One gate-and-arm rule.** `AbilityGate` owns the fired-arms / failed-arm-cools
   fallback; abilities keep numbers, lines and windows.
4. **One hit plan.** `MeleePlan.planHit` decides bump, Rush peek, Rend peek, Impetus
   and the combined mult in field order; the entry applies in the same order.
5. **Model-only card draw.** The renderer's definition overload is deleted; parts stay
   as the pinned decision seams, the screen and renderer cross the model seam alone.
6. **Windows carry their one-shots.** Pending flags and the Rend snapshot move into
   `Window`; reset clears the set, not the fields.

## Consequences

- Triggers are more testable (plain longs, no lane object); `CooldownsTest` still pins
  the lanes directly.
- The arming invariant is asserted once (`AbilityGateTest`) instead of five times over.
- New windows join one clear loop instead of N field lines; lanes keep their own verbs.
- Cost: triggers changed shape (one test updated), and `HitPlan` peeks what the entry
  consumes — same `now`, so peek and consume agree.
