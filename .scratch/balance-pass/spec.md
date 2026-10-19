# Spec: Balance pass — one unit per stat family

Status: done — shipped in 0.4.0 (beta); hand checks seen in the 1.21.11 client log

## Problem Statement

Three units for the same kinds of knobs, and one copy contradiction:

1. **Copy contradiction on movement speed**: Momentum says `-2%/-4%/-7%`, Scutum `about -6%`, Offload `+10%`, Roulette `+0.02` — one stat, three renderings, all `ADD_VALUE` flats against the 0.1 base.
2. **Damage power is split**: pre-mitigation attribute flats (`Burden`/`Gladius`/`Aequilibrium` `+1`, gone through the tick writer) versus post-mitigation settle mults (stances, wheel, dice). A flat is kit-dependent (`+1` ≈ x1.25 on a wood sword, x1.12 on netherite) and cannot be compared across classes or kits.
3. **The charged bonus curve is dead math**: its only caller (`MeleePlan`'s Onslaught price) is gated on `isFullSwing` first, so `bonusFor` always evaluates at full charge. The gate is the real timing rule; the curve is a leftover from when a flat chip paid at any charge.

The first-cut budget model (method below, superseded by v2) showed the class scores spread 0.89–1.26 of the mean — outside the ±10% target the grill locked; the tuning round closed it.

## Decisions (user-set, grill 2026-10-19)

- **Target**: the 3 live classes plus the classless baseline; no class outside ±10% expected payout, checked at the bare and iron-armored references; niches keep their shape.
- **Unit rule**: damage power = `x`; attribute stats = flat; chances = `%`; stacks = int; seconds. One unit per family, forever.
- **Conversions**: `Burden`, `Gladius`, `Aequilibrium`, `Onslaught` `+1` damage → `x1.15` each; all fold multiplicatively into the melee plan's combined mult.
- **Onslaught**: the mult pays any charge; the 2-stack price stays per full-swing hit; the window and bar rules (15s or bar emptied, no feed while live) are unchanged.
- **Movement**: stays flat (`ADD_VALUE`); copy goes flat everywhere; values rounded now: Momentum `-0.005/-0.01/-0.015`, Scutum `-0.005`, Offload `+0.01`, Roulette `+0.02`; `ConditionalPassive.percent` deleted.
- **BonusCurve**: deleted, `bonusFor` and its tests included; every "charged bonus" doc line goes; the gate moves to `FullSwing.isFull(charge)`.
- **Process**: the pass proceeds on top of the uncommitted round already in the tree (user's call).

## Budget method (v2)

- Reference kit: iron sword (6 damage, 1.6 attacks/s), no armor, no effects, 20 HP, flat melee.
- Offense **and** defense are measured on the same 600s timeline (user's call), both reported:
  - Offense: sustained full-charge DPS; kit windows fire on their lanes and pay their prices (Onslaught needs 10 Momentum and spends 2 a full swing, Offload spends 5, Roulette spins free, All In on cooldown), so the bar's spend and the feed suppression are priced in.
  - Defense: effective HP vs the classless iron attacker, armor read per hit — Momentum ramps its checkpoints (and sheds them passing through Onslaught), Scutum reads plate + Testudo on its 8s flip cycle, Roulette Ward rides its window. Armor DR via the 1.21.11 `CombatRules` formula: `reduction = clamp(armor - damage/(2 + toughness/4), armor*0.2, 20)/25`, max 80%.
- Score = DPS x EHP. The **official band is ±10% around the mean of the five rows** (user's call); the pairwise max/min is printed for context.
- Not priced yet: knockback resistance, mobility, lane collisions, multiple attackers, off-class gear. `.scratch/balance-pass/model.py` recomputes the table.

## Budget table (after the tuning round)

| class | DPS | EHP | score | rel mean |
|---|---|---|---|---|
| classless | 9.83 | 20.0 | 196.8 | 0.94 |
| Juggernaut (swing -0.4) | 9.52 | 21.2 | 201.5 | 0.96 |
| Legionary Gladius (take x1.2) | 13.82 | 16.7 | 230.6 | 1.10 |
| Legionary Scutum | 9.10 | 21.3 | 193.3 | 0.92 |
| Gambler (crit 15%, All In x1.75) | 11.43 | 20.1 | 229.5 | 1.09 |

Candidate mean 210.4; the ±10% band is 189.3–231.4 — every row inside (worst pair 1.19, Gladius/Scutum). Measured rows for reference: Juggernaut today (swing -0.5) 185.8 (0.88); Gladius today (take x1.1) 251.1 (1.19); Gambler today (crit 20%, All In x2.0) 234.6 (1.12); Gladius with alacrity +0.1 instead 223.7 (1.06).

## Tuning round (issue 05, user's calls 2026-10-19)

- Metric: offense and defense both measured (band around the mean is official).
- Juggernaut: Burden swing -0.5 → **-0.4** (keeps the slow-heavy read, gives back rate).
- Gladius: glass tax x1.1 → **x1.2** (sharpens the trade; alacrity +0.1 was the runner-up at 1.06).
- Scutum: **unchanged** — Testudo's cycle and the plate put it at 0.92 once defense is priced.
- Gambler: crit base 20% → **15%**, All In x2.0 → **x1.75** (Streak step and betrayal stay).

## Implementation Decisions

- New `FullSwing.java` (`isFull(charge)`) replaces `BonusCurve`; `PassiveStacks`, `CritRoll`, `MeleePlan` and `ServerEvents` call through it.
- `ClassDefinition.AlwaysOn` gains `dealMult` (default 1.0): the chassis row's melee mult. `Burden` is the first carrier; the plan reads each always-on row's mult.
- `RotateAbility`: Gladius aura drops the damage attribute and gains `GLADIUS_DEAL_MULT` x1.15 read by `dealMult(stance)`; Scutum stays x0.9; `SCUTUM_WEIGHT` -0.005.
- `AequilibriumAbility`: window aura keeps only armor; gains `DEAL_MULT` x1.15, read while the oath window is live.
- `OnslaughtAbility`: `HP_PER_HIT` becomes `DEAL_MULT` x1.15; `MeleePlan` pays it on any landed hit while live and prices 2 stacks on full swings only.
- `MeleePlan.HitPlan` loses `onslaughtHp` and gains the price flag; `combined` = chassis x stance x wheel x oath x onslaught, with take and dice as today. `Landed` carries verdict + combined only; `DamageSettle.dealBonus` is deleted with its last caller.
- Copy renders each family in its unit: `x1.15 damage`, `-0.005 speed`, `%` stays chances only.

## User Stories

1. As a player reading the picker, I want every movement row in one unit, so that I can compare kits.
2. As a player, I want a class's damage power to read the same at every weapon tier, so that a mid-game pick does not secretly change power.
3. As the maintainer, I want one home for the full-swing gate and no dead curve code, so that the timing rule cannot drift.
4. As the maintainer, I want a budget table and pinned constants, so that the next rebalance starts from numbers.

## Testing Decisions

- Pure JUnit: `FullSwing` gate exactness; converted constants (x1.15 x4, flat grid); plan composition (chassis x stance x wheel x oath x onslaught); Onslaught any-charge pay + full-swing price; Aequilibrium window mult; flat copy generation; Momentum tiers.
- `BalanceBudgetTest` pins the declared constants (not a simulation).
- In-game hand checks per `AGENTS.md`: melee round touching spear thrust and shield-eaten hits (both contracts), and the `DamageSettle` guarded hurt (a x1.15 extra lands as one blow: one sound, flash, knockback, combined `lastHurt`).

## Tickets

- `01-full-swing-module` — delete the curve, move the gate. (implemented)
- `02-damage-mult-conversion` — the four conversions, plan composition, price split, settle cleanup. (implemented)
- `03-movement-flats` — values, flat copy, helper deletion. (implemented)
- `04-docs-and-changelog` — GLOSSARY, guide, CHANGELOG, ADR-0028. (implemented)
- `05-tuning-round` — budget table reconciliation with the user (settled: knobs above; pinned in `BalanceBudgetTest`).
