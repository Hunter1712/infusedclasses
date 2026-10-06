# Spec: War Cry debuff shout

Status: done

## Problem Statement

Warrior lost its offence active with the deletion: Second Wind sustains, Berserk rewards
low HP, Bulwark armors up — but nothing answers getting swarmed. The player wants War Cry
back with a new job: a shout that softens the pack, so positioning buys time instead of
tanking every hit at full damage and full enemy speed.

## Solution

New Warrior active on `R`: a 16-block shout hitting hostile mobs only with Weakness I and
Slowness I for 8s each. Own 60s cooldown lane, no Bulwark cost, pure debuff with no self
buff. Decide-then-apply throughout: a pure trigger/fire gate-and-arm decision mirroring
Second Wind, one adapter that announces and writes the effects. No HUD row, no signal —
vanilla effect icons are the display.

## User Stories

1. As a swarmed Warrior, I want one shout to weaken and slow the pack, so that I can reposition instead of dying in place.
2. As a Warrior pressing `R` off cooldown, I want Weakness + Slowness on nearby hostiles, so that the pack hits weaker and reaches me slower.
3. As a Warrior browsing the picker, I want the War Cry rule text with radius and duration, so that I know what it does before choosing.
4. As a classless player pressing `R`, I want silence, so that the key does nothing for others.
5. As a Warrior pressing `R` on cooldown, I want the remaining-seconds line, so that I know the wait.

## Implementation Decisions

- `WarCryAbility`: `NAME` "War Cry", `RADIUS_BLOCKS` 16.0, `EFFECT_TICKS` 160 (8s), Weakness and Slowness at amplifier 0, carried as effect ids (`minecraft:weakness`, `minecraft:slowness`) rather than holders so the decision stays bootstrap-free. Kinds FIRED/WRONG_CLASS/ON_COOLDOWN, `trigger` gates read-only, `fire` gates and arms the new lane together. Announcement reuses the old line: `War Cry!` in red.
- Gating goes multi-ability: `ClassDefinition.activeAbility` (singular) becomes `activeAbilities: List<Identifier>`. Both decisions scan it; locked validation rejects a non-empty list, playable requires one. Registry order is War Cry then Second Wind (the old card order).
- `Cooldowns` regrows its second lane in the current two-field style (no inner class): `WAR_CRY_MS = 60_000L`, `useWarCry`/`warCryRemaining`, `reset` readies both. `PlayerRuntime` exposes the lane through verbs, mirroring the Second Wind ones.
- `WarCryPayload` unit intent (`infusedclasses:war_cry`), `R` keybind plus `en_us.json` entry, receiver in the entry mirroring the Second Wind one (no consume step — nothing is spent).
- `WarCryApplier.apply(player, result)`: announces (reusing `AbilityApplier`'s path, widened, not duplicated), then scans `player.getBoundingBox().inflate(RADIUS)` for alive `Monster`s and writes each carried effect. Caster excluded by the scan's except-arg; players never match the predicate, passive animals never match the type.
- Registry Warrior actives gain War Cry first with copy built from the constants (`Weakens and slows hostiles within 16 blocks for 8s (R, 60s)`-style). Second Wind numbers untouched.
- Glossary regains War Cry (new meaning), Cooldown goes two-lane again, Class/Active cover two actives.

## Testing Decisions

- Pure JUnit on the decision: wrong class silent, on-cooldown reports remaining, fire carries radius + both effects + announcement and arms only its own lane, rejects arm nothing, effect constants exact.
- Cooldown independence: both lanes advance separately, rejects never arm, reset readies both.
- Registry/presentation: Warrior carries both ability ids in order, tooltip joins the War Cry line with honest numbers, card shows both actives.
- Not tested: entity scan, effect pixels, keybind feel, balance — manual dev-client verification.

## Out of Scope

- Self buff on the shout (Berserk/Bulwark own the caster).
- Affecting players, passive animals, or tamed mobs.
- HUD row or expiry signal for the debuffs.
- Touching Bulwark numbers, Berserk curve, Second Wind heal/cooldown, or max-HP bonus.
- Extending/refreshing semantics beyond vanilla `addEffect` overwrite.
