# 02: Crooked Dice + Streak

Spec: `.scratch/gambler/spec.md`

**Blocked by:** 01.

**Status:** ready-for-agent

## Task

The Gambler's dice, verified pure (the class is not pickable until 05):

- New `CritRule` record on the class definition: crit chance, crit mult,
  betrayal chance, betrayal mult, streak step. Mechanical update:
  `ClassDefinition` gains the component and the existing Juggernaut/Legionary
  entries pass the universal rule (e.g. `CritRule.universal()`; implementer's
  call).
- `CritRoll` extends to the class override — Gambler bands: betrayal
  `[0, 0.05)`; crit `[0.05, 0.05 + min(0.95, 0.20 + 0.15 × streak))`;
  remainder normal. Betrayal is fixed forever; the crit band reaches 0.95 at
  streak 5 and stays there. The universal path is untouched.
- Streak: `PlayerRuntime` gains a counter with read/bump/reset verbs. +1 per
  non-crit full-charge landed hit (betrayal counts as a non-crit); any crit
  resets it; the existing switch/respawn reset wipes it with the rest. The hit
  plan carries the verdict, the applied multiplier and the streak movement;
  the hit settle applies the movement where it already applies stack bumps and
  spends.
- Betrayal: folds ×0.5 into the same deal-mult settle path as the crit (the
  discount lands pre-armor, so it is shallower against armored targets — keep
  the caveat documented, not fixed). Betrayal gets its own cue, distinct from
  the crit cue.
- Card: `ClassPresentation` renders `Crooked Dice` and `Streak` lines
  generated from the rule (streak reset target = chance base, lock computed),
  so copy cannot drift.
- Tests: extend `CritRollTest` (band cuts at 0.049/0.05/0.249/0.25 at streak
  0; +1 on normal, +1 on betrayal, reset on crit; 95/5 lock at streak 5 and
  beyond); extend the runtime test (streak verbs, wipe), the plan test
  (movement returned to the settle) and the presentation test (generated
  lines).

## Acceptance

- Pure JUnit: band boundaries, streak bump/reset/wipe, betrayal settle,
  generated card lines; `gradle test` green.
- Nothing outside the Gambler rule path changes: Juggernaut and Legionary
  keep the universal baseline byte-for-byte.

## Comments

None yet.
