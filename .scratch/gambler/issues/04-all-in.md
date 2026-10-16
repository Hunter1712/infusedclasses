# 04: All In

Spec: `.scratch/gambler/spec.md`

**Blocked by:** 02 (the ultimate only makes sense on the Gambler's crit rule).

**Status:** ready-for-agent

## Task

The Gambler's ultimate, verified pure:

- New `AllInAbility` in the house gate-and-arm shape: fires only for Gamblers;
  arms the 75s lane (`ALL_IN_MS`, `useAllIn` / `allInRemaining`) and a 15s
  window together. Rejects arm nothing. Announces on fire.
- Crit decider: while the All In window is live the crit multiplier reads 2.0
  instead of the rule's 1.5. Everything else stays: betrayal keeps its 5% and
  ×0.5, a crit still resets Streak, every band is unchanged — the window lifts
  one number and bends no rule.
- Dispatch: `BindMorph` gives the `G` row an All In target for Gamblers
  (`REND | AEQUILIBRIUM | ALL_IN`); the receiver dispatches the existing
  payload, no protocol change.
- Tests: new `AllInAbilityTest` (fire arms lane + 15s window; rejects arm
  nothing; independent of the Roulette lane); extend `CritRollTest` (×2.0
  while live, betrayal and Streak reset unchanged).

## Acceptance

- Pure JUnit: window lift, lane independence, reject paths, morph target;
  `gradle test` green.

## Comments

None yet.
