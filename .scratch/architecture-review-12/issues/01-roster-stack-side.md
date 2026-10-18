# 01 — The roster carries the stack's view

Status: done

## Context

`AbilityRoster` owns an ability's word, lane read and binds, but which payload window and
pending flag ride beside its lane is restated in `StatusHud`'s `signatureRow`/`ultimateRow`
chains (three signature branches, three ultimate branches, each with its own window and
pending rules). Round-12 candidate 1.

## Deliverable

- `Row` grows `View view`: the payload lane read plus its rider rules
  (`wanted(status)` / `remaining(status)` / `label(status)`). One rider for most rows; two
  for Rotate (Impetus pending, Testudo flip); the label-carrying one for Roulette.
- `StatusHud` keeps only the class-shape pick — `signatureLane(def)` / `ultimateLane(def)`
  returning a row — and renders every lane line from the row: lane chip from
  `view.laneRemaining`, riders from the rules.

## Done when

- Drift pins: every row's view lane read equals its runtime lane read after `of` on an
  armed-lanes fixture; each rider reads its own window on an all-distinct payload.
- `StatusHudTest` passes unchanged (the behaviour pin).
