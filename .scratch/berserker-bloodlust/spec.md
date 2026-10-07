# Spec: Berserker Bloodlust (rename + War Cry replacement)

Status: ready-for-agent

## Problem Statement

Warrior's kit does not play as a comeback brawler and its name collides with
its passive. Three findings from the grill:

1. **Name collision**: class `Warrior` carries a passive group `Berserk`
   (`Berserk Damage` / `Berserk Speed` in `ClassRegistry.java:50-61`). Renaming
   the class to Berserker forces a passive rename.
2. **War Cry debuff is flat**: Weakness I + Slowness I for 10s in 16 blocks
   (`WarCryAbility.java:20-23`, scan `WarCryApplier.java:54-56`) is
   fire-and-forget with no loop into Bulwark/Second Wind beyond the +5 builder.
3. **Armor-shred rejected**: `armor` is `RangedAttribute(default 0, min 0,
   max 30)` and `AttributeInstance.calculateValue()` ends in `sanitizeValue()`,
   so `0 + (-X)` reads back `0`. Negative armor is unexpressible via modifiers,
   and `CombatRules` math would only grant `+8%` taken for `-10` anyway.

## Solution

Role stays **comeback brawler** — weak full, scary low. Full rename plus a
lifesteal opener:

- **Rename**: `Warrior` → `Berserker`, full rename (id
  `infusedclasses:berserker`, display `Berserker`). Passive group `Berserk` →
  `Fury` (`Fury Damage` / `Fury Speed`), numbers unchanged. Old
  `infusedclasses:warrior` picks map forward to Berserker on load, no wipes.
- **Bloodlust (R, 30s)**: 10s lifesteal window. Heal per melee hit is
  `damageTaken * bucket * 0.02` (0% at full HP → 20% at 0 HP via
  `HpScaledPassive.bucket()`), any melee damage counts, capped at 2 HP per hit,
  no window cap (duration is the cap). Fires only when `bucket >= 5`
  (~under 5 hearts at 20 max); above that fizzles free like Second Wind at 0
  stacks (cooldown untouched, no root, no builder, `Too healthy`-style line).
- **Builder scales, on-hit does not**: `R` grants `bucket` Bulwark stacks
  (0 full → 10 zero, 5 at half = old flat 5), cap-respecting via the existing
  `onTrigger` path, grace re-stamped. `on_hit_taken` stays 1 per hit.
  Self-root (`Spellcast` 1s Slowness III) lands on FIRED only.
- **Second Wind (G, 60s)** unchanged: consume-all → absorption shield.

## User Stories

1. As a full-HP Berserker, I want `R` above 5 hearts to fizzle free, so that a
   fat-finger costs nothing and the dead press is honest.
2. As a bleeding Berserker, I want `R` to open a 10s window plus `bucket`
   Bulwark, so that the opener seeds what hits build and `G` spends.
3. As a low-HP Berserker mid-window, I want any melee hit to heal
   `damage * bucket * 2%` (max 2 HP/hit), so that staying aggressive is the
   comeback without spawner immortality.
4. As a swarmed Berserker, I want hits taken to still build Bulwark 1-per-hit,
   so that the loop works while Bloodlust heals.
5. As a returning Warrior pick, I want to load as Berserker, so that the rename
   strands nobody.
6. As a picker browser, I want copy generated from the new constants
   (Berserker, Fury, Bloodlust 10s/30s, gate, rate, caps, builder curve), so
   that text cannot drift from behaviour.

## Implementation Decisions

- `ClassRegistry`: `WARRIOR` → `BERSERKER`, id
  `infusedclasses:berserker`, display `Berserker`, colour/tagline unchanged;
  `HpScaledPassive` names `Fury Damage` / `Fury Speed`, group `Fury`,
  per-steps `0.3` / `0.05` unchanged; actives
  `[bloodlust, second_wind]` with copy built from constants.
- New `BloodlustAbility`: `NAME` "Bloodlust", `WINDOW_TICKS` 200 (10s),
  `LIFESTEAL_PER_BUCKET` 0.02, `MAX_PER_HIT_HP` 2.0, `GATE_BUCKET` 5,
  kinds FIRED/WRONG_CLASS/ON_COOLDOWN/TOO_HEALTHY; `trigger` pure read-only
  (class gate → gate bucket → cooldown gate), `fire` gates and arms together.
  Rate helper `lifestealFor(bucket, damageTaken) = min(2.0, damage * bucket *
  0.02)`, zero at bucket 0 by construction.
- `Cooldowns`: `WAR_CRY_MS` 20s → `BLOODLUST_MS` 30_000L with
  `useBloodlust`/`bloodlustRemaining`; `reset` readies both lanes.
  `PlayerRuntime` exposes the lane plus the Bloodlust window expiry
  (press timestamp; live = `now - pressedAt < 10s`); respawn/switch clears it
  with everything else.
- Grant path: firing credits `bucket` Bulwark via the existing
  `grantWarCryStacks`-shaped path (renamed), capped by `maxStacks` (20),
  grace re-stamped through `onTrigger`; rejects grant nothing.
- Lifesteal path: new attacker branch on `AFTER_DAMAGE` in `ServerEvents`
  (victim path keeps Bulwark bump): source player is a Berserker with a live
  Bloodlust window and the damage is melee → `player.heal(capped)`. No
  charge gate (any melee), self-damage excluded by attacker-is-player check.
- `AbilityApplier`: roar kept, announce `Bloodlust!`, root via shared
  `Spellcast` path on FIRED only; heal feedback is hearts puff per heal
  (implementer's call on count); no mob scan, no radius.
- Copy/docs regenerate from constants: `ActiveAbility` descs,
  `HpScaledPassive.fullDesc` (Fury), plus `GLOSSARY.md`
  (Warrior→Berserker, Berserk→Fury, War Cry→Bloodlust entries).
- Migration: `PlayerClasses` load maps stored `infusedclasses:warrior` to
  `infusedclasses:berserker`.

## Testing Decisions

- Pure JUnit on decisions: lifesteal math at buckets 0/5/10 × damage 7/30
  (0 HP, ~0.7 HP, cap clip to 2.0); gate blocks bucket 4 / fires bucket 5;
  builder 0/5/10 at buckets 0/5/10; fizzle/too-healthy/wrong-class arm nothing;
  fire arms only its own lane.
- Cooldown independence: 30s Bloodlust and 60s Second Wind advance separately,
  rejects never arm, reset readies both, respawn clears window.
- Registry/presentation: Berserker carries both ability ids in order, old
  Warrior id resolves forward, tooltip/copy reflects new numbers.
- Not tested: heal pixels, keybind feel, pack-balance — manual dev-client
  verification (`gradle deployClient`, `/class select <player>`).

## Out of Scope

- Touching Bulwark numbers (20 × 0.5, grace 5, lose-all), Fury per-steps,
  Second Wind shield/cooldown/fizzle, max-HP bonus (stays 0).
- Window cap (duration is the cap), charge gate, ranged/spell lifesteal.
- Renaming Second Wind, touching Ranger/Mage, respawn/logout semantics
  beyond window clear, HUD rows.
- Balance tuning beyond these numbers — next round after playtest.
