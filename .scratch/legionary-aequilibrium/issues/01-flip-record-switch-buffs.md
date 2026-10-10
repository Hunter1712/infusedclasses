# 01: Flip record + Impetus/Testudo switch buffs (runtime + melee plan)

Spec: `.scratch/legionary-aequilibrium/spec.md`

**Blocked by:** none.

**Status:** done

## Task

Flip pays under the old names. `PlayerRuntime.flipStance()` stamps nothing
today:

- Add one flip record (`flipAt` millis + `flippedTo` stance) in
  `PlayerRuntime`. New signature `flipStance(now, landing)` stamps both.
  `resetCooldowns` clears it with the windows and resets stance to `SCUTUM`.
  Collapse the two oath windows into the single Aequilibrium window (ticket
  03 owns the new ability; this ticket owns the flip record).
- `ImpetusAbility`: repurpose to the Gladius-flip buff. Delete `IMPETUS_BONUS`;
  add `SWITCH_DEAL_MULT` 1.10, `SWITCH_DEAL_MS` 5_000. Trigger reads the flip
  record (flipped to `Gladius` inside 5s), not the oath lane. One-shot
  peek/consume verbs mirror Rush (`consumeRushBonus`). Any charge per
  agreement. Consumed on the hit settle, expires at 5s.
- `TestudoAbility`: repurpose to the Scutum-flip buff. Delete `TESTUDO_ARMOR`
  8.0, `TESTUDO_DEAL_MULT` 0.7 and the plate/brace aura; add
  `SWITCH_TAKE_MULT` 0.85, `SWITCH_TAKE_MS` 1_500. Trigger reads the flip
  record (flipped to `Scutum` inside 1.5s), not the oath lane.
- `MeleePlan`: `dealFor` multiplies 1.10 while the Impetus flip buff is
  pending; `takeFor` multiplies 0.85 while the Testudo flip window is live.
  Old oath-window branches move out in this ticket. Move the three
  `flipStance()` callers together (`RotateAbility.fire`, tests).

## Acceptance

- Pure JUnit: flip-to-Gladius arms Impetus, consumed exactly once, expires at
  5s, any charge; flip-to-Scutum arms Testudo, live/expiry at 1.5s,
  Scutum-gated; flip record cleared on reset; stance still defaults to
  `SCUTUM`.
- `gradle test` green.

## Comments

Landed: flip record (`flipAt`/`flippedTo`) + Impetus one-shot pending on `PlayerRuntime`,
`flipStance(now, landing)` stamped with the Rotate lane arm, `MeleePlan` deal/take flip
branches, old oath-window branches out. Deviation: Impetus/Testudo `Result`/`trigger`/`fire`
deleted outright (nothing fires them once `G` means Aequilibrium, so the old
announcements went with the oath path); the buffs peek the flip record instead.
