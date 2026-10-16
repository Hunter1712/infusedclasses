# 03: Roulette

Spec: `.scratch/gambler/spec.md`

**Blocked by:** 01 (independent of 02 — both touch the lanes/runtime files, so
expect rebase churn if run in parallel).

**Status:** ready-for-agent

## Task

The Gambler's free wheel, verified pure:

- New `RouletteAbility` in the house trigger/fire gate-and-arm shape: one roll
  per spin over seven bands, in order Edge 0–0.20, Ward 0.20–0.40, Haste
  0.40–0.60, Vigor 0.60–0.80, Jackpot 0.80–0.88, Bust 0.88–0.96, Zero
  0.96–1.00. Free: no HP cost, no self-damage anywhere. Rejects (wrong class,
  on cooldown) arm and spend nothing. Fired spins announce their outcome.
- Lanes: `Cooldowns` gains the Roulette 25s lane (`ROULETTE_MS`, `useRoulette`
  / `rouletteRemaining`, same shape as the existing lanes).
- Window: `PlayerRuntime` gains the Roulette outcome window (payload = the
  outcome, 10s), wiped by the existing switch/respawn reset with the rest.
- Effects at fire: Vigor heals 3 HP, Jackpot heals 4 HP (instant). While the
  window is live (10s): Ward +2 armor and Haste +0.02 speed stat the player
  through the passive applier's window-modifier path; Edge ×1.25, Jackpot
  ×1.25 and Bust ×0.75 fold into the melee deal multiplier, read by the plan
  (any charge, like stance mults).
- Dispatch: `BindMorph` gives the `R` row a Roulette target for Gamblers
  (`RUSH | ROTATE | ROULETTE`); the receiver dispatches the existing payload,
  no protocol change. The ability carries `desc()` generated from its
  constants for the 05 copy.
- Tests: new `RouletteAbilityTest` (band cuts at 0.199/0.20, 0.399/0.40,
  0.599/0.60, 0.799/0.80, 0.879/0.88, 0.959/0.96, 1.0 → Zero; free fire arms
  lane + window; wrong-class and on-cooldown rejections arm and spend nothing;
  no path touches Streak; desc generated); extend the cooldown, runtime and
  bind morph tests.

## Acceptance

- Pure JUnit: band cuts, lane/window arming, reject paths, mults read by the
  plan, morph target; `gradle test` green.
- No HP cost machinery anywhere in the ability.

## Comments

None yet.
