# ADR-0009: Oath dispatch stays in the server entry

Status: accepted (point 3's shared-marker keying is superseded by ADR-0026; point 1's switch home moved to `SlotPresses` — the shape is unchanged)

## Context

The architecture review proposed giving `BindMorph` a polymorphic fire path so the
server entry's `G`-receiver switch (Impetus/Testudo/Rend) collapses to one call, with
the Rend consume riding inside.

## Decision

1. The entry keeps the three-way switch. `BindMorph` decides what a reused bind means
   (`OathTarget`); the entry maps that answer to a decision plus its adapter, and
   Rend's consume stays a separate adapter call gated on the result's consume flag.
2. Pushing dispatch into the morph stops it being a pure decision: it would need the
   player (announce, cast sound, spend), which is the exact seam ADR-0001 draws. A
   three-way switch on a three-value enum in the wiring module is the shallowest
   correct shape, not friction.
3. What the review correctly found ships anyway: both morph checks key on the same
   Legionary marker (`ROTATE`) through `ClassDefinition.hasAbility`, so routing can
   no longer disagree with itself.

## Consequences

- Future reviews should not re-propose morph dispatch without a fourth oath target
  that actually duplicates the switch body (today the Impetus/Testudo arms differ by
  type, not by shape; a shared helper would only rename `announceAndCast`).
- Cost: the entry keeps ~20 lines of explicit wiring, and Rend's consume protocol
  stays visible at the call site instead of hidden in a dispatcher.
