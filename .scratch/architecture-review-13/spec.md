# Spec: Architecture review round 13 — implement the seven deepening candidates

Status: done

## Goal

Round 13 (report + walk, Oct 18 2026) surfaced seven deepening opportunities after round 12's
five. All seven land as one commit on `main`; nothing a player does changes meaning — the wire
shape, the HUD rows, the cooldown boundaries, the ability dispatch and the attribute reconcile
are all pinned by the tests that already existed.

## Decisions

- **01 — the snapshot walks the lanes.** `PlayerStatusPayload` loses its 23 per-lane components
  and the `Field`/`ArmedField`/`ExactField`/`IgnoredField` table with them: it carries
  `Map<Lane, LaneRead>` plus the seven facts that are not per-lane (class id, flip target,
  Roulette outcome, Momentum stacks/grace/stamp, stance, Streak). `of`, `encode`, `decode` and
  `movedSince` each loop over `Lane.values()`; a seventh window costs zero new fields.
- **02 — the lane is the key.** `Lane` becomes a top-level enum owning its cooldown length
  (moved off `Cooldowns`), and every per-lane list keys off it: `Cooldowns` is a lane-keyed
  `use`/`remaining`/`reset` triad, `PlayerRuntime` holds `Map<Lane, Window>` with one
  `arm`/`rotate`/`roulette`/`consumeBonus`/`consumePayload` set replacing six joint arms, six
  consumes and a hand-written reset array, and `PlayerRuntime.State` carries the same
  `Map<Lane, LaneRead>` the snapshot does. The abilities name their own window constants at
  their (new) arm sites, so ADR-0013's "once, at its arm site" becomes literally true.
  ADR-0025 records it.
- **03 — one class shape.** `BindMorph`'s walk and `StatusHud`'s hardcoded candidate lists
  become one derivation: the target enums carry the ability each slot fires, `signatureFor` /
  `ultimateFor` walk the class's actives through those ids, and `shape(def)` returns both
  targets plus the rows that draw them. The ultimate no longer keys on the ROTATE row — its
  lending was inert (the lent ability rejects for a class that does not carry it) — and the
  drawn ultimate row is the fired target's own row. ADR-0026 records it.
- **04 — one press path.** The two intent receivers share one prologue (`press`: class read,
  classless bail, runtime lookup), each slot's switch keeps its arms per ADR-0009, and Rend's
  landing moves out of the wiring into a `RendApplier` beside `RushApplier` — the two
  abilities whose landing is more than the announce.
- **05 — the switch cleanup is a decision.** `PassiveApplier.cleanupFor(oldDef, newDef)`
  answers every modifier a switch must take off (stack passives, the Vigor pair and the
  window-aura families in one list, keyed by modifier id) so the wiring only removes and the
  rule is JUnit-reachable; `apply`'s `def == null` early return and the mirrored loops die.
- **06 — the wrap is a decision.** `ClassCardRenderer.wrapRows`/`measure` take a
  `ToIntFunction<String>` width instead of a `Font`, so the greedy wrap, the tone boundary and
  the scroll layout are assertable with a stub measure; the screen passes `font::width`.
- **07 — the trigger is a value.** `Trigger` is a record carrying its id plus its picker copy
  (`earn`/`idle`), `ConditionalPassive` carries it, and `TriggerPhrasing` and the bare
  `Trigger`/`no trigger` fallback die: a trigger cannot be spelled inert (two comparisons that
  never match) and a new trigger adds one constant with its own words.

## Landing order

07 (base for 01/02's call sites), 02, 01, 03, 04, 05, 06. 01 and 02 are one change in the
code; 03 depends on 02's `Row` (the lane, the riders). Ticket numbers follow the report's
candidate numbers, not the landing order.

## Tickets

- 01 — `issues/01-snapshot-walks-lanes.md`
- 02 — `issues/02-lane-is-the-key.md`
- 03 — `issues/03-one-class-shape.md`
- 04 — `issues/04-one-press-path.md`
- 05 — `issues/05-cleanup-decision.md`
- 06 — `issues/06-wrap-decision.md`
- 07 — `issues/07-trigger-value.md`
