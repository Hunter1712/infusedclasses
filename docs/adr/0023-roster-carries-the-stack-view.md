# ADR-0023: The roster row carries the stack's view

Status: accepted (point 1's `View` and point 2's ultimate-bind sentence are revised by ADR-0025 and ADR-0026)

## Context

Round 11 (its candidate 1) built `AbilityRoster` as the one table of the abilities the mod
fires: a row carried the gating id, the Title Case word, the lane read over the runtime
state and the slot binds. ADR-0021 then made the stack speak words and ride windows beside
their lanes. But which payload window and pending flag rides beside which lane — and what
the rider means — was still restated per ability inside `StatusHud`'s signature and
ultimate chains: six branches, each naming its own payload field, its own flag and its own
label.

The pairing had no single owner. A row's word and lane were one fact in two places the
moment the HUD drew them; a window paired with the wrong lane would compile and draw; and a
fourth ability would edit a fourth surface to exist.

## Decision

The stack's view of an ability lives on its roster row:

1. **A row carries a `View`**: the payload lane read, plus its rider rules — `wanted`
   (whether the rider draws), `remaining` (the window it reads) and `label` (its words,
   usually none). One rider for most rows; Rotate carries two (Impetus pending, the Testudo
   catch); Roulette's rider carries the band word.
2. **The rows decision keeps only the class-shape pick**: which row is the signature and
   which is the ultimate stays in `StatusHud`, because the ultimate bind keys on the
   `ROTATE` row while the drawn row is Aequilibrium's (ADR-0009) — binds cannot select
   display rows. Everything a lane line draws comes from the row.
3. **The lane read stays split in two**: server-side consumers (`/class info`, `BindMorph`)
   read the runtime state, where the stack reads the payload a client holds. Two reads, one
   row; the roster is where they sit beside each other.

## Consequences

- A new ability adds one row with its rider rules; the rows decision needs no new branch and
  names no ability's payload fields.
- The roster now depends on the payload type — accepted: the payload is the client's whole
  view, and the roster is the one table that explains it.
- Drift pins: roster tests read lane agreement across the two sides (armed fixture through
  `of`) and each rider's own window on an all-distinct payload; `StatusHudTest` pins the
  drawn stack unchanged.
