# ADR-0024: One silent reject

Status: accepted

## Context

ADR-0013 point 2 built the outcome envelope with a machine-readable `remainingMs` and a
separate `onCooldown` factory; ADR-0017 stripped the cooldown line, and ADR-0019 kept the
remainder anyway ("still pays for it"). Since then nothing read it: the applier ignores
anything without a line, the status sync rebuilds every lane remainder from the runtime, and
the six abilities' own `Result` rows restated the same split (`wrongClass()` vs
`onCooldown(ms)`) only to hand back the same silent pair.

Round 12 (ticket 02) deleted the remainder and collapsed the factories; the tests that had
observed it now pin the lane clock from the runtime instead.

## Decision

A rejected press is one shape: `AbilityOutcome.rejected()` — nothing said, nothing granted,
nothing armed. The envelope is `AbilityOutcome(fired, announcement)` with `fired` /
`rejected` / `fizzled` factories; abilities with a payload keep wrapping it, and their
`Result` rows carry one `rejected()`.

## Consequences

- ADR-0013 point 2 is superseded on `remainingMs` and `onCooldown` (noted there).
- ADR-0017's silence becomes total: the reject carries no data at all, so no consumer can
  grow a second presentation of it.
- A cooldown echo (a number on screen) would re-add data deliberately, with the ADR that
  asks for it.
