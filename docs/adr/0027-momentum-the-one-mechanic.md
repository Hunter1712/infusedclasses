# ADR-0027: Momentum is the Juggernaut's one mechanic

Status: accepted

## Context

The Juggernaut accumulated three parallel systems: Momentum (armor per landed hit, all-at-once
drop), Vigor (HP-scaled attack bonuses), and a Rush/Rend pair where Rush fed the bar and Rend
spent every stack in one next-hit payout. The kit read as several unrelated rewards; the bar was
a prerequisite for Rend rather than the thing the class played around, and the charged quadratic
curve existed only to price Rush's bonus hit and Rend's payout.

## Decision

1. **Momentum is the class's one mechanic.** `Momentum` owns the ids and numbers: a 60-stack bar,
   +1 on a full-swing hit dealt and +1 on a hit taken, 5s grace, then -1/s. `ConditionalPassive`
   carries `earns`, `maxStacks`, `tiers` and `graceSeconds`; `Trigger` narrows to
   `on_hit_dealt` and `on_hit_taken`.
2. **Three checkpoints, read off the bar every tick.** 15 Steady (+1 armor, -2% speed, +0.25
   knockback resistance), 30 Braced (+3/-4%/+0.5), 45 Rooted (+5/-7%/+1.0). Past 45 stays
   Rooted; 45-60 is the actives' spend reserve. Knockback resistance is on every tier.
3. **Burden is the always-on chassis.** +1 attack damage, -0.5 attack speed, written while
   classed and never earned. `ClassDefinition.alwaysOn` carries it; Vigor is cut and the
   HP-scaled machinery stays registered nowhere.
4. **Offload replaces Rush on the signature slot.** A flat 5-stack price, a 10s lane and a 5s
   window of +0.2 attack speed and +10% move speed; below the price it fizzles with
   `No Momentum`, and an unspent window owes nothing back.
5. **Onslaught replaces Rend on the ultimate slot.** A 10-stack gate, a 60s lane and a 15s
   window that also ends when the bar empties; each full-swing hit pays +1 damage and spends 2
   stacks, and nothing feeds the bar while it is live.
6. **The charged bonus curve retires.** `BonusCurve` keeps only `isFullSwing`; Rush, Rend and
   their appliers are deleted.
7. **The wire keeps its names.** `RushPayload`/`RendPayload`, their channels and the R/V keys
   keep their old names; only the lanes and ability ids change
   (`infusedclasses:offload`, `infusedclasses:onslaught`, `Lane.OFFLOAD`, `Lane.ONSLAUGHT`).

## Consequences

- A landed hit prices itself once: `MeleePlan.HitPlan` carries `onslaughtHp`, suppresses the
  stack bump while Onslaught is live, and `Landed` lands exactly what the settle spent.
- A shield-eaten hit earns nothing on either side: `resolveTaken` gates on landed damage > 0,
  and the melee feed stays behind the same gate.
- The bar's quiet bleed is visible: `PassiveStacks` decays on its own clock, `decay` catches up
  whole overdue seconds in one step, and the snapshot's grace field is time-to-next-loss.
- Supersedes ADR-0004 (Vigor and the Rush/Rend kit) and the charged-curve half of ADR-0006; the
  stack gate and full-swing rule stand. Retires the Rush/Rend adapter splits of ADR-0008, the
  `rush_speed`/Rend-pair vocabulary of ADR-0021, and renames ADR-0026's printed fallbacks to
  Offload and Onslaught.
- Drift pins: `MomentumTest`, `ConditionalPassiveStacksTest`, `OffloadAbilityTest`,
  `OnslaughtAbilityTest`, `MeleePlanTest`, `ClassSwitchCleanupTest`, `StatusHudTest`.
