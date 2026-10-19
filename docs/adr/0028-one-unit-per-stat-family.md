# ADR-0028: One unit per stat family

Status: accepted

## Context

The balance grill found the same kinds of knobs expressed three ways: movement speed copy mixed
`-2%`, `about -6%`, `+10%` and `+0.02`; damage power split between pre-mitigation attribute flats
(`Burden`/`Gladius`/`Aequilibrium` `+1`, kit-dependent: +1 is x1.25 on a wood sword and x1.12 on
netherite) and post-mitigation settle mults (stances, wheel, dice); and the charged bonus curve was
dead math — its only caller double-gated on full swing, so `base x charge x charge` always
evaluated at `base x 1`. A first-cut budget model (`.scratch/balance-pass/`) put the class scores
0.89-1.26 of the mean, outside the ±10% target.

## Decision

1. **One unit per stat family.** Damage power is a multiplier (`x`), composed on landed damage;
   every attribute stat is a flat value in that attribute's own unit; chance knobs are percents;
   resource knobs are integers; time is seconds. `ADD_VALUE` stays the only operation written to
   attributes.
2. **The four damage flats convert.** `Burden`, `Gladius`, `Aequilibrium` and `Onslaught` `+1`
   damage become `x1.15` each (the iron-sword reference match), folded multiplicatively into the
   melee plan's combined mult. The chassis mult rides `ClassDefinition.AlwaysOn.dealMult`; stance
   mults stay on `RotateAbility.dealMult`; `Aequilibrium` and `Onslaught` window mults are read
   from their lanes.
3. **Onslaught splits pay from price.** The mult pays on any landed hit while the window is live;
   the 2-stack price stays per full-swing hit; the window and bar rules are unchanged.
4. **The curve retires, the gate moves.** `BonusCurve` is deleted with `bonusFor` and its tests;
   `FullSwing.isFull(charge)` (exact `>= 1.0F`) is the one gate, still shared by the stack feed,
   the crit roll and the Onslaught price.
5. **Movement speed is flat, and the copy says so.** `Momentum` `-0.005/-0.01/-0.015`, `Scutum`
   `-0.005`, `Offload` `+0.01`, `Haste` `+0.02`; `ConditionalPassive.percent` is deleted, so no
   movement row claims a percentage again.
6. **Balance has a target and a sheet.** No class outside ±10% expected payout (DPS x effective
   HP at the iron-sword kit) at the bare and iron-armored references; the sheet and its model live
   in `.scratch/balance-pass/` and `BalanceBudgetTest` pins the declared constants.

## Consequences

- Reverses ADR-0006 point 1 and ADR-0027 point 6: no flat bonus damage caller remains, so the
  curve and its docs go. The full-swing stack gate of ADR-0006 stands, now named `FullSwing`.
- Flats landed pre-mitigation and mults land post-mitigation (`DamageSettle`): at a bare target
  the conversion is value-matched, against armor the reference table's two-ref check covers the
  drift.
- Spam inside a live Onslaught window pays the mult without a price; the bar only drains on full
  swings and the bleed, which is the accepted shape of "pay any charge, price full swings".
- `DamageSettle.dealBonus` is deleted with its last caller; the guarded hurt stays for every
  mult settle.
- Drift pins: `BalanceBudgetTest`, `MeleePlanTest`, `OnslaughtAbilityTest`, `LegionaryTest`,
  `MomentumTest`, `DamageSettleTest`.
