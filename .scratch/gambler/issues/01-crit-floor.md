# 01: Crit floor — retire vanilla crits, universal full-swing crit

Spec: `.scratch/gambler/spec.md`

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

## Task

Crits become a mod rule:

- Mixin: the existing `PlayerMixin` cancels `Player.canCriticalAttack` for
  every player (head inject, return false). That one gate owns the vanilla
  ×1.5, the `PLAYER_ATTACK_CRIT` sound and the crit particles (animate action
  4) together. Mobs untouched; `magicCrit`/ENCHANTED_HIT (the enchanted-hit
  sparkle, not a crit) untouched. The mixin holds zero policy — no per-class
  allowance, no `vanillaAllowed` seam.
- New pure decision `CritRoll`: the verdict for one landed full-charge hit —
  crit or normal — from the class's crit rule (this ticket: the universal
  baseline, 10% crit at ×1.25, no betrayal, no streak movement), the swing
  charge and a roll the caller drew. Classless reads as the baseline. Half
  swings verdict none. No random source inside the decision.
- Entry: `ServerEvents` draws one roll per landed full-charge hit, for every
  player — classless included — and passes it into the hit plan; sweeps draw
  per swept target. The settle folds the crit multiplier into the swing's deal
  multiplier through the existing second-hurt settle on the pre-armor
  `baseDamageTaken` (armor reduces both parts; the bonus drops when the base
  hit already killed the target).
- Classless: the attacker settle no longer returns early for players without a
  class. A classless full-charge landed hit settles the universal crit only —
  no stacks, no windows, no streak, no betrayal.
- Feedback: a crit verdict replays the vanilla cue from the settle adapter
  (animate action 4 plus the crit sound). No HUD, no per-hit chat.
- Tests: new `CritRollTest` (universal boundaries at 0.099/0.10, mult 1.25,
  half-charge none, classless = baseline, no movement); extend the melee plan
  test (verdict and multiplier on the hit record, crit folded into the deal
  mult, Rend bonus untouched) and the settle/entry test for the classless
  branch.

## Acceptance

- Pure JUnit: universal boundaries, charge gate, classless branch, crit folds
  onto the settle; `gradle test` green.
- No behaviour change for mobs or the enchanted-hit path.

## Comments

None yet.
