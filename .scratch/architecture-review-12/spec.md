# Spec: Architecture review round 12 — implement the five deepening candidates

Status: done

## Goal

Round 12 (report + walk, Oct 18 2026) surfaced five deepening opportunities after round 11's
six. All five land as one commit on `main`; behaviour is unchanged throughout.

## Decisions

- **01 — the roster carries the stack's view.** A row grows a `View` half: the payload lane
  read plus its rider rules (`wanted` / `remaining` / `label`). `StatusHud` keeps only the
  class-shape pick (which row is the signature, which the ultimate) and renders every lane
  line from the row. One table; `ClassInfo` and `BindMorph` are untouched.
- **02 — the silent reject is one shape.** `AbilityOutcome(fired, announcement)` with one
  `rejected()`. The machine-readable remainder and the per-ability `onCooldown`/`wrongClass`
  factories die; the distinction they carried was unobservable since ADR-0017.
- **03 — `ON_HIT_TAKEN` is deleted, not wired.** No adapter has fed it since Bulwark left, so
  re-declaring a hit-taken passive should fail at compile time rather than go silently inert
  (the ADR-0005 failure mode).
- **04 — class identity travels resolved.** The tick passes the `def` and `runtime` it already
  holds into `maybeSend`; join and respawn keep resolving internally (classless included, no
  runtime materialised); `PlayerStatusPayload.of` takes the definition, never an id string.
- **05 — the payload field table's rows are typed by move kind.** `ArmedField` /
  `ExactField` / `IgnoredField`; the `(Long)` cast in `movedSince` dies. The round-11
  all-distinct round trip stays the pin.

## Tickets

- 01 — `issues/01-roster-stack-side.md`
- 02 — `issues/02-reject-one-shape.md`
- 03 — `issues/03-delete-dead-trigger.md`
- 04 — `issues/04-class-identity-once.md`
- 05 — `issues/05-typed-field-rows.md`
