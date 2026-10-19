# Spec: Juggernaut Momentum (heavy wall rework)

Status: draft

## Problem Statement

The Juggernaut kit is three borrowed ideas and no identity:

1. Rush is a dive-and-cleanse engage that a wall never needed; Rend is a plain
   feed-then-spend loop. Both read like the old Berserker, not like a tank.
2. Momentum is linear armor per stack with a 20 cap: no checkpoints, no cost.
3. Vigor rewards high HP, an axis the class does not earn: nothing about the
   tank loop is about staying topped.
4. The wanted chassis (+ damage, - attack speed) was scrapped and then re-added
   by the user: the class must feel heavy first, fast never.

User's requirement: one mechanic, Momentum, a bar with checkpoints the actives
spend; a slow heavy tank where more armor means more weight.

## Decisions (user-set)

- Momentum bar, 0 to 60. Hit dealt +1, hit taken +1. Decay: 5s quiet, then
  lose 1 stack per second. Death clears.
- Three tiers, no fourth: `15 Steady` +1 armor, -2% speed, +0.25 knockback
  resistance; `30 Braced` +3, -4%, +0.5; `45 Rooted` +5, -7%, +1.0. Everything
  past 45 stays Rooted; the headroom to 60 is reserve for the spend actives.
  Armor, weight and resistance are the tier's totals, not cumulative adds.
- The chassis passive is always on: +1 attack damage, -0.5 attack speed. Its
  name is **Burden**.
- R is **Offload**: spend 5 stacks, 5s, +0.2 attack
  speed, +10% move speed, 10s lane. Small and spammable: the load comes off
  the shoulders, the swing loosens and the step quickens.
- V is **Onslaught**: spend 2 stacks per full-charge landed hit for +1 damage
  each; no stack gain from any source while live; ends at the empty bar or 15s,
  whichever comes first; 60s lane. Casting requires 10 stacks (dial).
- Spending drops stacks, not tiers: the tier is read from the bar every tick
  and follows it down. Nothing drops a tier as an event.
- Rush and Rend retire. Vigor is cut (provisional: keeping it is one registry
  line).

Loop: land hits and take hits to build -> tiers buy armor and sell speed ->
Offload spends the bar to set the weight down -> Onslaught dumps
the bar into damage. Armor follows the bar down; the leftover stacks are what
you keep.

## User Stories

1. As a Juggernaut, I want one bar that everything reads and spends, so the
   class has one thing to learn.
2. As a Juggernaut under fire, I want taking hits to feed the bar, so being the
   wall pays.
3. As a loaded Juggernaut, I want checkpoints with names and visible armor, so
   I can feel the tier I am holding.
4. As a Juggernaut who disengaged, I want the bar to bleed slowly, so a fight
   is a conversation and not a permanent score.
5. As a Juggernaut chasing, I want Offload to cost a little Momentum for a
   little speed, so I can stick to a target without being fast.
6. As a loaded Juggernaut, I want Onslaught to turn the bar into damage over a
   window the bar itself measures, so the spend is the fight.
7. As a Juggernaut during Onslaught, I want to gain nothing, so the window is
   a commitment, not a buff.
8. As a picker browser, I want copy generated from the tier table and the
   ability constants, so text cannot drift from behaviour.

## Implementation Decisions

- `Momentum` (new pure module): cap 60; earns `ON_HIT_DEALT` +1 (full swings
  only) and `ON_HIT_TAKEN` +1; three tier rows with thresholds, names, armor,
  weight (absolute move-speed deltas `-0.002/-0.004/-0.007`) and knockback
  resistance (`+0.25/+0.5/+1.0`); `forStacks(stacks)`, `allModifiers()` (every
  row's armor/weight/resistance), card copy built from the earns, the rows and
  the decay rate. Modifier ids: `infusedclasses:momentum_armor`,
  `infusedclasses:momentum_weight`, `infusedclasses:momentum_knockback` (add
  the `knockback_resistance` registry key beside the existing ones).
- Chassis module `Burden` (file `Burden.java`): +1.0 `attack_damage`
  (`infusedclasses:burden_damage`), -0.5 `attack_speed`
  (`infusedclasses:burden_swing`), desc generated from the constants.
- `Trigger`: add `ON_HIT_TAKEN = new Trigger("on_hit_taken", "Take a hit")`.
  `ON_HIT_DEALT` copy stays (`Land a hit`). The record carries `(id, earn)`; the idle
  word goes because the grace line is the passive's, not a trigger's.
- `ConditionalPassive` reshapes to the one live shape: `(String name,
  List<Earn> earns, int maxStacks, List<Tier> tiers, int graceSeconds)` with
  `Earn(Trigger, int stacks)` and `Tier(int threshold, String name,
  double armor, double weight, double knockback)`. Per-stack attribute, per-stack
  value and single-attribute fields go. Momentum is built from `Momentum`'s
  constants. `fullDesc` generates from the earns, the rows and the decay
  ("Land a hit: +1. Take a hit: +1. Max 60. 5s no feed: -1/s. 15 Steady: +1
  armor, -2% speed. ...").
- `ClassDefinition`: new `AlwaysOn(String name, String desc,
  List<PassiveEffect.Modifier> modifiers)` list on the definition (Juggernaut
  carries the chassis). `writtenBy` includes it, so switch cleanup removes it;
  the card draws it as a passive line; the tick writes its modifiers while
  classed.
- `PassiveStacks`: decay returns as the per-second step: a passive keeps its
  last-feed stamp and a next-decay stamp; past `graceSeconds` it loses one
  stack per second until empty (no drop-all). `onTrigger(def, trigger, charge)`
  bumps by the matching `Earn` amount, capped, and re-stamps the grace; new
  `spend(passive, n)` spends `min(n, stacks)` and returns the amount. Decay
  math is a pure step (`stacks`, `nextDecayAt`, `now`) so JUnit covers it
  without a clock.
- `PassiveEffect`: keeps the plan shape (`Modifier`, `Write`/`Remove`/`Skip`,
  `forBucket`/`planForBucket`) for HP-scaled buckets. The conditional bar's
  tier-to-modifier mapping lives in `Momentum.forStacks`/`allModifiers`: one list for
  the reached row and one full sweep set, so write and remove cannot drift. Zero
  stacks wants nothing and removes.
- `PassiveApplier`: tick reconciles the Momentum bar through that mapping (the one
  conditional bar today; another bar adds its own mapping module and a dispatch,
  never reuses Momentum's ids), each `AlwaysOn` writes its modifiers, and the window
  families reconcile as today plus Offload. `consumeSpent` (the Rend path) leaves
  with Rend.
- `PlayerRuntime.State`: `rendBonusStacks` goes; nothing takes its place
  (Onslaught reads its lane and the stack count). Add `spendStacks(passive,
  n)` and `endWindow(Lane)` (the Onslaught early end at an empty bar).
- `BonusCurve`: `bonusFor` stays as the universal flat-bonus rule for every
  class and classless players alike (`base x charge x charge`, clamped);
  Onslaught's +1 is the current caller, and the full-swing gate still blocks
  spam. `isFullSwing` remains the stack/crit/Onslaught gate.
- `MeleePlan`: `HitPlan` drops `rushBonus`, `rendStacks`, `rendHp`; adds
  `onslaughtHp` (1.0 when the Onslaught window is live and the swing is a full
  swing, else 0). The bump is suppressed while the Onslaught window is live
  (no dealt gain). New `resolveTaken(victimDef, victimRuntime, landedDamage,
  now)`: damage taken > 0, melee only, feeds `ON_HIT_TAKEN` (+1), suppressed
  while the victim's Onslaught window is live; classless and unaided victims
  cost nothing. `settle` spends 2 stacks per qualifying Onslaught hit and
  `endWindow`s at zero; `Landed` carries `onslaughtHp` to the entry.
- `ServerEvents`: attacker branch lands `DamageSettle.dealBonus(...)` with
  `onslaughtHp` (the Rend line becomes this); the victim feed runs in both
  branches — mob melee victim branch and, for a Juggernaut struck by a player,
  in the attacker branch — through `MeleePlan.resolveTaken`. Shield-eaten hits
  (`damageTaken == 0`) earn nothing anywhere, per the existing guard. The tick's
  snapshot re-reads state when the empty-bar end fires, so a closed window never
  draws a rider for one more tick.
- `OffloadAbility` (new): name "Offload",
  `COST_STACKS` 5, window 5s, lane `Lane.OFFLOAD` 10s, fizzle line "No
  Momentum" below 5, gate-and-arm; fire spends 5 through
  `runtime.spendStacks`. Window family (marker the Offload ability) writes
  `infusedclasses:offload_haste` (+0.2 attack speed) and
  `infusedclasses:offload_speed` (+0.01 move speed) while live. Cast sound:
  the wind burst holder (same as Rush's).
- `OnslaughtAbility` (new): name "Onslaught", `MIN_STACKS` 10, `COST_PER_HIT`
  2, `HP_PER_HIT` 1.0, window 15s, lane `Lane.ONSLAUGHT` 60s; gate-and-arm, no
  modifiers of its own (its lane read is the whole state). Cast sound:
  `RAVAGER_ROAR` holder. Fire line "Onslaught!", fizzle "No Momentum"; the
  desc names the empty-bar end as well as the 15s.
- `Lane`: `RUSH(25s)` -> `OFFLOAD(10s)`, `REND(60s)` -> `ONSLAUGHT(60s)`.
  `Cooldowns` verbs follow. The wire keeps its names on purpose:
  `RushPayload` is the signature intent, `RendPayload` the ultimate intent,
  channels `infusedclasses:rush` / `infusedclasses:rend` unchanged.
- `BindMorph`: signature target OFFLOAD and ultimate target ONSLAUGHT for the
  Juggernaut; the Rush/Rend fallback names retire with the abilities. Keys
  `R`/`V` and client labels unchanged.
- `AbilityRoster`: rows OFFLOAD and ONSLAUGHT replace RUSH and REND; both draw
  their window rider while live (countdown-only when the window is spent, so
  no stale rider).
- Payload/HUD: the grace fields stay (decay is back, so their countdown is
  real); `StatusHud.rows` keeps the Momentum row and adds the live tier word as
  a bare rider (Roulette band-word precedent), so the checkpoint shows on the
  stack.
- `ClassRegistry`: Juggernaut passives `[Momentum.passive()]`, hp passives
  `[]` (Vigor cut), `alwaysOn` `[Burden.chassis()]`, actives
  `[OFFLOAD_ABILITY, ONSLAUGHT_ABILITY]`; the `rush`/`rend` ability ids go, the
  new ids are `infusedclasses:offload` and `infusedclasses:onslaught`.
- Docs: `GLOSSARY.md` (Momentum rewrite, the chassis name, Offload, Onslaught;
  Rush, Rend, Vigor retire from the language), `docs/guide/classes.md`,
  `CHANGELOG.md`, and an ADR for the momentum-only rework. Run
  `tools/doc-flags.sh` and `tools/arch-flags.sh` before calling a round done;
  expect `./gradlew compileJava` and `./gradlew test` after every edit batch.

## Testing Decisions

Pure JUnit, no player needed:

- `Momentum`: boundaries 0/14/15/29/30/44/45/60 give armor 0/0/1/1/3/3/5/5,
  weight 0/0/-0.002/-0.002/-0.004/-0.004/-0.007/-0.007, knockback resistance
  0/0/0.25/0.25/0.5/0.5/1.0/1.0; `allModifiers` holds every row's ids for the
  sweep.
- Feeds: dealt +1 full swing, taken +1, cap 60 holds at 60; spam (half charge)
  earns zero; `damageTaken == 0` earns zero; classless inert; no trigger means
  no passive.
- Decay step: quiet 5s loses nothing; 5s and 100ms past grace loses 1; 10s
  past grace loses 5; a feed at any point re-stamps and restarts the bleed;
  the floor is 0 and never negative.
- Spend: `spend(passive, 5)` at 7 leaves 2 and reports 5; at 3 leaves 0 and
  reports 3; `endWindow` clears the Onslaught lane read.
- `OffloadAbility`: 4 stacks fizzles with the line and arms nothing; 5 stacks
  fires, spends 5, arms the 10s lane and the 5s window; a second press while
  the lane cools is rejected silently; the family wants both modifiers only
  while live.
- `OnslaughtAbility`: 9 stacks fizzles; 10 fires and arms 60s/15s; drain on a
  full swing takes 2 and lands `onslaughtHp` 1.0; spam lands 0 and drains 0; at
  the empty bar the window ends and the lane stays cooling; no dealt and no
  taken gain while the window is live.
- `MeleePlan`: existing stance/impetus/dice paths untouched; the released
  Rush/Rend fields are gone from `HitPlan`/`Landed`.
- Cleanup: `cleanupFor` removes the chassis, the tier ids and the Offload pair
  on a class switch, and keeps what the new class re-earns; classless runs
  write nothing.
- Registry/presentation: Juggernaut carries `[offload, onslaught]` in order;
  copy spells thresholds, names, decay and the chassis numbers; no Rush, Rend
  or Vigor text remains in copy.
- Not automatable: sound audibility, speed and weight feel, knockback
  resistance in hand, drain visuals — hand check on the dev client
  (`./gradlew deployClient`, `/class select <player>`, then that instance's
  `logs/latest.log`): the HUD shows Momentum plus the tier word, Offload sets
  the weight down, Onslaught drains the bar per swing and ends at empty, and a
  shield-eaten hit earns nothing.

## Dials

- Knockback resistance values per tier (0.25/0.5/1.0) and whether the top
  tier reads as full immunity in hand.
- Chassis attack speed stays -0.5 unless it feels too slow in hand.
- Tier thresholds and values; Offload 5/5s/10s/+0.2/+0.01; Onslaught
  10/2/+1.0/15s/60s; decay 1 stack per second.
- Vigor cut is provisional. Keeping it is one registry line plus its copy.

## Out of Scope

- Ranged or projectile feeds for Momentum (melee only, same direct-entity
  check as today); Vigor alternatives; balance tuning past these numbers; new
  keybinds or wire channels; the other class pitches.
