# 02 — Damage mult conversion

Status: done
Spec: `.scratch/balance-pass/spec.md`

## What

Convert the four `+1` damage flats to `x1.15` post-mitigation mults and fold every attacker-side mult into the plan's combined.

## Where

- `ClassDefinition.AlwaysOn` gains `double dealMult` (canonical constructor plus a 3-arg overload defaulting 1.0). Javadoc: the chassis row's melee mult, read by the plan, never written to attributes.
- `Burden`: drop `DAMAGE_BONUS`/`DAMAGE_ID`; add `DEAL_MULT = 1.15`; `chassis()` carries the swing modifier only and `dealMult = DEAL_MULT`; `desc()` renders `x1.15 damage, -0.5 attack speed`.
- `RotateAbility`: drop `GLADIUS_DAMAGE`/`GLADIUS_DAMAGE_ID`; add `GLADIUS_DEAL_MULT = 1.15`; `GLADIUS_AURA` keeps alacrity only; `dealMult(stance)` returns 1.15/0.9; `desc()` renders `x1.15 damage`.
- `AequilibriumAbility`: drop `EDGE_BONUS`/`EDGE_ID`; add `DEAL_MULT = 1.15`; window aura keeps plate; add `dealMult(PlayerRuntime.State)` reading the oath window; `desc()` renders `x1.15 damage`.
- `OnslaughtAbility`: `HP_PER_HIT` becomes `DEAL_MULT = 1.15`; add `dealMult(state, stacks)` = `liveFor ? 1.15 : 1.0`; `desc()` says the mult pays any hit and the price stays per full swing.
- `MeleePlan`: `HitPlan` loses `onslaughtHp`, gains `boolean priceOnslaught`; `deal` = chassis x stance x wheel x oath x onslaught; `combined` = deal x take x dice as today; `settle` spends on `priceOnslaught`; `Landed` = verdict + combined.
- `ServerEvents`: drop the `DamageSettle.dealBonus` call and comments that name the flat bonus.
- `DamageSettle`: delete `dealBonus`; javadoc still owns the guarded hurt.

## Tests

- `MeleePlanTest`: onslaught assertions move from `onslaughtHp` to `deal`/`combined` and the price flag; spam inside the window pays the mult and spends nothing.
- `OnslaughtAbilityTest`: pay-any-charge, price-full-swing, no-feed-while-live.
- `LegionaryTest`: Gladius 1.15 deal via `dealMult`, aura lists without `gladius_damage`, Aequilibrium mult in the oath window.
- `ClassRegistryTest` / `ClassSwitchCleanupTest`: chassis row shape and cleanup without the damage id; `WindowFamilyTest` if it pins the aura lists.
- `DamageSettleTest`: drop `dealBonus` cases.

## Done when

`./gradlew test` green; no `HP_PER_HIT`, `DAMAGE_BONUS`, `GLADIUS_DAMAGE`, `EDGE_BONUS` references; in-game hand checks per the spec.
