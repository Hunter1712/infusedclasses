# Spec: Warrior balance (comeback brawler)

Status: done

## Problem Statement

Warrior's kit doesn't play as a comeback brawler. Three findings from the balance grill:

1. **Triple-stacked tankiness**: +20 max-health AND +10 Bulwark armor AND a heal — no identity, just stats.
2. **Second Wind fights Berserk**: it heals flat HP, but Berserk recomputes from live HP
   every tick (`PassiveApplier.apply`), so healing turns off the damage bonus. Heal-to-live
   vs low-to-kill is an anti-synergy, and 10 HP on a 40 HP pool (25%) makes the
   spend-all-armor trade feel bad.
3. **Boring actives**: both lanes 60s = press once per fight, forget. War Cry is a free
   fire-and-forget debuff with no loop into Bulwark/Second Wind.

## Solution

Role, locked by grill: **comeback brawler** — weak at full HP, scary at low HP.
Opener → build → spend loop: War Cry opens and seeds Bulwark, taking hits builds it,
Second Wind cashes it out as a shield while Berserk stays hot.

- **Base**: remove the +20 max-health bonus (back to vanilla 20 HP). Bonus HP was the
  sacred cow; grill released it — the shield below replaces it as the survival tool.
- **Bulwark**: `on_hit_taken`, 20 max × +0.5 armor (ceiling unchanged at +10, smoother
  per-hit steps), 5s grace, lose-all.
- **Berserk**: linear 10 steps unchanged, 0.3 damage / 0.05 speed per step
  (max +3.0 / +0.5). Speed nerfed harder than damage — it multiplies damage.
- **War Cry (R, 20s)**: Weakness I + Slowness I for 10s, 16 blocks, **grants 5 Bulwark
  on press** (builder). Self-root kept (Slowness III 1s, one rule for all casts).
- **Second Wind (G, 60s)**: consume all Bulwark → **absorption `stacks × 1.0 HP` for
  10s** (max 20 HP = 100% of bar). HP stays low so Berserk stays on — spend and
  damage point the same way. Fizzle at 0 stacks unchanged, name kept (no rename churn).

## User Stories

1. As a swarmed Warrior, I want `R` to soften the pack AND seed 5 Bulwark, so that the
   opener starts my loop instead of just debuffing.
2. As a brawler taking hits, I want each hit to add +0.5 armor in small steps, so that
   building to 20 feels gradual, not bursty.
3. As a low-HP Warrior, I want linear Berserk (+0.3/+0.05 per 10% missing) to feel like
   Sharpness II at half HP and scary near death, without one-shotting packs.
4. As a low-HP Warrior with stacks, I want `G` to shield me for 10s without healing,
   so that I keep my Berserk bonus while I finish the fight.
5. As a Warrior with zero Bulwark, I want `G` to fizzle with `No Bulwark` and arm
   nothing, so that a fat-finger costs nothing.
6. As a Warrior browsing the picker, I want copy generated from the new constants
   (20-stack Bulwark, absorption shield, 10s War Cry, 20s lane), so that text can't
   drift from behaviour.

## Implementation Decisions

- `ClassRegistry`: Warrior `maxHealthBonus` 20.0 → 0.0; Bulwark `ConditionalPassive`
  10 × 1.0 / grace 5 → 20 × 0.5 / grace 5; Berserk per-steps 0.4/0.08 → 0.3/0.05.
- `Cooldowns.WAR_CRY_MS` 60s → 20s; `SECOND_WIND_MS` stays 60s.
- `WarCryAbility.EFFECT_TICKS` 160 → 200 (10s); radius 16.0 and amplifier 0 unchanged.
  Grant path: firing arms the lane AND credits 5 Bulwark (cap-respecting, re-stamps
  grace) — same decide-then-apply split, grant resolved in the applier, not the entry.
- `SecondWindAbility`: result carries absorption HP (`stacks × 1.0`, max 20) + 10s
  window instead of heal HP; `consume` semantics and fizzle unchanged. Exact
  absorption application (effect amplifier mapping vs direct amount — 4 HP/level
  granularity does not divide 1.0 HP/stack evenly) to be resolved against the
  1.21.11 jar at implementation time.
- `AbilityApplier`: apply shield instead of `player.heal()`; hearts puff stays (or
  switches to an absorption-appropriate particle — implementer's call).
- Root kept on both actives (`Spellcast` untouched — no per-ability exception).
- Copy/docs regenerate from constants: `ClassDefinition.ActiveAbility` descs,
  `ConditionalPassive.fullDesc`, `HpScaledPassive.fullDesc`, plus `GLOSSARY.md`
  (Class / Bulwark / Berserk / War Cry / Second Wind entries all change).

## Testing Decisions

- Pure JUnit on decisions: Bulwark cap/grace math at 0/5/20/21 stacks; Berserk
  bucket × new per-steps; War Cry fire carries 10s window + 5-stack grant and arms
  only its lane; Second Wind absorption math + fizzle arms nothing; consume clears
  stacks and armor plan.
- Cooldown independence: 20s War Cry and 60s Second Wind advance separately,
  rejects never arm, reset readies both.
- Registry/presentation: Warrior carries both ability ids in order, tooltip/copy
  reflects new numbers, card shows both actives.
- Not tested: entity scan, absorption pixels, keybind feel, pack-balance — manual
  dev-client verification (`gradle deployClient`, `/class select <player>`).

## Out of Scope

- Renaming Second Wind → Last Stand (name kept deliberately).
- Backloaded/frontloaded Berserk curves (linear kept).
- Partial-spend or damage-spender variants (full-spend shield kept).
- Root exemption for War Cry (one rule for all casts).
- Touching Ranger/Mage, respawn/logout semantics, HUD rows.
- Balance tuning beyond these numbers — next round after playtest.
