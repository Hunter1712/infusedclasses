# Spec: Warrior procs (War Cry extension + Bulwark heal)

Status: superseded — War Cry + extension deleted, Bulwark HUD deleted; Warrior keeps Bulwark armor + Second Wind only until a new offence active lands.

## Problem Statement

Warrior has one fire-and-forget active (War Cry: Strength II 8s, 30s cooldown, `R` key) and one conditional passive (Bulwark: `on_hit_taken`, +1 armor/stack, max 10, 5s grace). The class rewards staying in the fight with armor but gives no reason to keep swinging once the buff is up, and banked Bulwark stacks have no spender.

## Solution

Two procs alongside the existing kit, both decided purely and applied in adapters:

1. **War Cry extension**: fully-charged melee hits while War Cry's Strength is live roll 25% per victim to extend its remaining duration by 2s. No cap — sustained combat can hold Strength II indefinitely; death, milk, and disengaging end it.
2. **Bulwark heal (Second Wind)**: second active on `G` with its own 60s cooldown. Heals `stacks * 1.0 HP` (half heart per Bulwark stack, max 10 HP) and consumes all Bulwark stacks plus the armor they granted. Zero stacks fizzles free with a `No Bulwark` line, cooldown untouched.
3. **War Cry HUD row**: a Bulwark-style custom row titled `War Cry` (no `II` — Strength II is implied) showing seconds-left plus a drain bar, synced event-driven on fire and every proc. The vanilla Strength box is hidden (iconless effect plus inventory filter), so the row is the only display.

## User Stories

1. As a Warrior mid-fight, I want charged hits to stretch War Cry, so that staying aggressive keeps my damage window open.
2. As a Warrior mobbed by enemies, I want cleaves to roll per victim, so that fighting crowds feels rewarding.
3. As a Warrior who spams clicks, I want only fully-charged swings to count, so that Java-edition timing matters.
4. As a Warrior low on health with full Bulwark, I want to cash out stacks for a heal, so that I choose between staying tanky and recovering.
5. As a Warrior with zero Bulwark, I want `G` to tell me `No Bulwark` without burning 60s, so that a fat-finger costs nothing.
6. As a Warrior with a beacon nearby, I want my own War Cry row, so that I can tell my uptime apart from foreign Strength.
7. As a player getting hit by procs, I want a small `minecraft:crit` puff on the victim, so that extensions read as hits, not re-fires.

## Implementation Decisions

- Extension decision is pure: `extendRoll(hasEffect, remainingTicks, roll)` — takes the roll in `[0,1)` so RNG stays in the adapter and JUnit asserts without it. Fires only when the attacker is a Warrior, the swing is fully charged (`attackStrengthScale == 1.0`), the live Strength instance exists, and `roll < 0.25`. Per-victim rolls: one sweep into N mobs is N rolls. No internal cooldown and no per-swing dedup; the charge gate (~0.6s swing timer) is the only throttle.
- Extension amount: +2s (40 ticks) per proc, re-applied via `addEffect` at amplifier 1 as `remaining + 40`. No cap. Source of truth is the live `MobEffectInstance` duration, never a runtime timestamp (milk/death clear the effect but not timestamps).
- Attacker-side hook reuses the existing `AFTER_DAMAGE` event in `InfusedClasses` (check source instead of victim). Exact `getAttackStrengthScale` signature to be confirmed against the 1.21.11 jar via `javap` at implementation time.
- Heal decision is pure: `healAmount(stacks) = stacks * 1.0 HP`; zero stacks yields a fizzle result with no cooldown arm. Spender semantics: consume all Bulwark stacks on fire — clear stacks plus last-trigger timestamps, remove the armor modifier via the passive applier, resend the stacks signal so no stale row sticks around.
- Cooldowns: War Cry stays `WAR_CRY_MS` 30s on `R`. Heal gets its own field plus `G` keybind, second C2S payload, and `refund`-style verbs kept separate — no shared cooldown. Rejected presses (wrong class, on cooldown, zero stacks) leave cooldowns untouched, matching the existing `trigger`/`fire` gate-and-arm shape.
- HUD: new S2C signal carrying War Cry expiry ticks on fire and every proc (event-driven, like the stacks signal); client store interpolates the countdown between messages; overlay row mirrors `PassiveHud.Row` shape with title `War Cry`. The Strength effect is applied iconless and filtered from vanilla's inventory list by the client mixin, so no vanilla box duplicates the row; milk still clears the effect and a beacon's Strength still shows.
- Farm exploit accepted: hostile mobs and players both roll; farm-buffed upkeep is intended Warrior fantasy for now, tuning later.

## Testing Decisions

- Pure JUnit on the decisions: extension fires only when charged + effect live + roll under rate; multi-victim independence is N calls, not one call; no-cap arithmetic (`remaining + 40`, no clamp); heal math at 0/5/10 stacks; fizzle arms nothing; consume clears stacks and armor plan.
- Cooldown machine: War Cry 30s and heal 60s advance independently; rejected presses never arm.
- Not tested: keybind plumbing, particle counts, overlay layout, mob-farm balance — manual dev-client verification.

## Out of Scope

- Deterministic/pity procs (raw 25% ships).
- Soft caps or diminishing returns past 16s (no cap ships).
- Throttled/tick-synced HUD updates.
- `attack_damage` stacking passives — War Cry stays the damage tool.
- Balance tuning beyond 25% / +2s / 1.0 HP-per-stack / 30s / 60s.
