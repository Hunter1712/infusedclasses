# 04 — One press path

Status: done

## Context

The signature and ultimate receivers each restate the same seven-line prologue (player, now,
`currentDefinition`, the classless bail with the same comment, the runtime lookup) around
their own switch, and `settleAttackerHit` grows a third copy of the "classless costs no
state" comment. Rend's landing (announce, cast, then the flag-gated `consumeSpent`) lives in
the wiring, so what a landing *is* for the two-most-landing path sits in the routing module;
`RushApplier` is the only ability with a landing adapter. Round-13 candidate 4.

## Deliverable

- One `press(player, slot)` helper: resolve the class once, bail classless (ADR-0005), resolve
  the runtime, hand the slot the three, so the comment and the sequence live once.
- The two slot switches stay in the entry per ADR-0009, one line per arm.
- `RendApplier` (new) owns Rend's landing — announce/cast plus the flag-gated consume — beside
  `RushApplier`, which keeps the cleanse and the announce; the wiring sequences nothing.

## Done when

- Both receivers are three lines of wiring each, and no receiver body repeats the classless
  comment.
- `RendAbilityTest`'s consume pins still hold; the landing paths are unchanged in effect and
  order (announce first, then the consume).
