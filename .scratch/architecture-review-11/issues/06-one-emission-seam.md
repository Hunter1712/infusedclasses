# 06 — The tick is the one steady-state emission change point

Status: done

## Context

`StatusSync`'s doc lists seven change points that must call `maybeSend`; two of them
(the receivers) were exactly where the classless leak lived, and the tick already
rebuilds and compares for every classed player every tick — so a missed immediate call
is ≤1 tick of latency, never staleness. Round-11 candidate 6 (speculative), whose ADR
callout said the seam must keep every coverage point before it earns its keep.

## Deliverable

- Delete the post-branch emissions (Rush receiver, Rend receiver, hit settle, class
  switch). Join and respawn keep their forced sends; logout keeps its forget.
- `StatusSync` doc rewritten around one steady-state change point (the tick: rebuild
  and compare, no packet when quiet) plus the two forced sends.
- ADR-0022 records the decision and the ≤1-tick latency tradeoff; ADR-0021's status
  line notes the superseded change-point carry.

## Done when

- `maybeSend`'s only steady-state caller is the tick; the suite is green; the in-game
  pass (`.scratch/ability-hud` ticket 05) confirms the stack still feels live at 20 Hz.
