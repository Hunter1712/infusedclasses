# Spec: Architecture review round 10 (flag from the 0.3.0 wave)

Status: needs-triage

## Problem Statement

AGENTS.md's architecture review trigger fired four times during the 0.3.0 wave (reviewed
against the base `63a2e35`): three files past the ~150-line bar and one module gaining more
than two public methods with the same noun. Each is spec'd by way of the wave's own ADRs, so
these are recorded flags for a later deliberate pass, not regressions to hot-fix.

## Candidates

1. `PlayerStatusPayload` (~200 lines): one record holding the 22-field snapshot, the builder,
   `movedSince` and the hand-rolled codec. ADR-0014 specced the single-value shape; the bar
   asks whether the codec half splits free (e.g. payload + separate serialization) without
   breaking the one-payload-one-truth rule.
2. `StatusHud` (~155 lines): rows per class plus chip construction. The roster is
   data-driven by design; watch whether a fourth class splits it rather than extends it.
3. `StatusHudElement` (~150 lines, at the bar): the draw half mirrors per corner with
   per-chip geometry. Candidate to deepen if a second draw consumer ever needs the geometry.
4. `PlayerRuntime` gained 7 same-noun `remaining`-family reads (six window remainders plus
   grace) on top of six lane reads. ADR-0014/spec 01 explicitly ordered these one-line
   forwards; the flag is only that the noun is now the dominant public surface — if a second
   consumer of the same shape appears, consider one `read(..., now)` seam instead of per-window verbs.
5. Judged but not flagged: the codec's two hand-maintained field orders (documented choice,
   ProtocolTest pins the round trip via equal-value boundaries), `ClassInfo.Lane`'s
   `ToLongBiFunction` boxing (single call site, one lane table).

## Out of Scope

- Hot-fixing any candidate during 0.3.0; the shape is what shipped and tests pin it.
- Moving the trigger bar; it did its job.
