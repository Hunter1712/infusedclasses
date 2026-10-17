# 05 — Classless players cost no runtime

Status: done

## Context

ADR-0005.1 promised "classless attackers still cost no transient state", but
`settleAttackerHit` (`ServerEvents`:193), both intent receivers, `StatusSync.snapshot`
and `/class info` look the runtime up unconditionally, materialising state for players
with no class. Round-11 candidate 5.

## Deliverable

- `settleAttackerHit`: the attacker runtime is looked up only when `atkDef != null`
  (the plan already handles a null runtime; classless hits still roll the universal
  dice and cue).
- Both intent receivers: a classless press returns before any lookup (it can only
  reject, and rejects move nothing).
- `StatusSync.snapshot`: `PlayerStatusPayload.classless()` when the player has no
  choice — the empty-class payload the client already hides on.
- `/class info`: classless draws the no-class line without a lookup.

## Done when

- The pure halves are pinned: `classless()` equals the empty snapshot shape,
  `MeleePlan.resolve` with a null attacker runtime settles the universal rule, and the
  wiring lookups are def-guarded.
