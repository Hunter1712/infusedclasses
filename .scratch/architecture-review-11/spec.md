# Spec: Architecture review round 11 — implement the six candidates

Status: done (the in-game pass rides `.scratch/ability-hud` ticket 05)

## Problem Statement

The round-11 review (report `architecture-review-20261017-125512.html`) found six seams
still carrying two sources of truth each. The user asked for all six to be implemented in
one wave:

1. Ability facts (word, lane read, slot bind) restated in `ClassInfo.LANES`, the six
   `StatusHud` word constants, `BindMorph`'s id checks and the payload's lane reads.
2. `PlayerRuntime` (418 lines, 44 public members, ~29 one-line forwards); the snapshot
   builder crosses 19 of them. Round-10 flag 4's "second consumer" has arrived several
   times over.
3. `PlayerStatusPayload` restates its 22 fields across record / `of` / `movedSince` /
   `encode` / `decode` (round-10 flag 1's field-order question).
4. `PassiveApplier` enumerates the window-aura families five times, and `RushApplier.tick`
   is a sixth mechanism for one modifier.
5. The classless promise (ADR-0005.1: "classless attackers still cost no transient state")
   leaks in the hit settle, both intent receivers, the status snapshot and `/class info`.
6. The status emission change points live in prose; the tick already rebuilds and compares
   for every classed player, so the immediate calls buy ≤50 ms and cost four call sites.

## Decisions

- Behaviour is unchanged throughout: every candidate is a seam move, not a rules change —
  with one recorded exception: emissions at press/settle/switch become tick-latent (≤1
  tick, 50 ms), recorded in ADR-0022, which supersedes the change-point carry in
  ADR-0020.4 / ADR-0021.7. Join and respawn keep their forced sends.
- ADR-0009.3 holds: the ultimate bind still keys on the `ROTATE` row, so the roster's
  `AEQUILIBRIUM` row carries the word and the oath lane read but no bind.
- ADR-0005.1 is restored by ticket 05, not amended.
- New vocabulary (GLOSSARY): **ability roster**, **runtime state**, **window aura family**.

## Tickets

- 01 — one ability roster table, five consumers
- 02 — `PlayerRuntime` behind one state read (the top recommendation)
- 03 — one field table in the snapshot payload
- 04 — one window-aura family list in `PassiveApplier`
- 05 — classless players cost no runtime
- 06 — the tick is the one steady-state emission change point

All six land in one commit on `main`; the in-game pass stays `.scratch/ability-hud` ticket 05.
