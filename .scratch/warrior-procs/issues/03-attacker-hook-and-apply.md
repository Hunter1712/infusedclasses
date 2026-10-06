# 03: Attacker-side extension hook plus heal fire path

**What to build:** Server wiring that applies the 01 and 02 decisions. Reuses the existing `AFTER_DAMAGE` event in `InfusedClasses` but checks the damage *source* (attacker) instead of the victim: fully-charged Warrior swings while Strength is live roll per victim and re-apply Strength at amplifier 1 for `remaining + 40` ticks. Second active gets its own `G` keybind, second C2S payload, and receiver that fires the 02 decision and applies heal plus consume. Every proc (and heal fire) emits a small `minecraft:crit` puff on the victim so extensions read as hits, not re-fires.

**Blocked by:** 01, 02

**Status:** ready-for-human

- [x] `AFTER_DAMAGE` handler splits cleanly: victim path keeps the existing Bulwark `on_hit_taken` bump; attacker path checks source player is a Warrior with a live Strength instance and `getAttackStrengthScale == 1.0` (exact 1.21.11 signature confirmed via `javap` against the cached jar at implementation time)
- [x] Extension applies via `addEffect` at amplifier 1 for `remaining + 40` ticks per proc, one roll per victim; hostile mobs and players both roll (farm upkeep is intended fantasy, tuning later)
- [x] No internal cooldown and no per-swing dedup; fully-charged gate (~0.6s swing timer) is the only throttle
- [x] Heal receiver: second C2S payload plus `G` keybind in `InfusedClassesClient`, own receiver in `InfusedClasses.registerReceivers`; rejected presses (wrong class, on cooldown, zero stacks) leave cooldowns untouched
- [x] Heal apply: `player.heal(stacks * 1.0f)`, then consume all Bulwark stacks (clear stacks plus timestamps, remove armor modifier through the passive-applier path, resend the stacks signal); zero-stack press sends `No Bulwark` and burns nothing
- [x] `minecraft:crit` puff emitted on the victim per proc; particle counts are manual-check only, not JUnit
- [ ] Keybind plumbing and farm balance are manual dev-client checks (see 05), not unit tests

## Comments

`javap` confirmed `Player.getAttackStrengthScale(float)`; the adapter calls it with `0.0F` and
rolls with the attacker's own random. The adapter builds one `Hit` carrying every gate — Warrior,
charged, own-Strength-live (amplifier 1, so beacon Strength I arms nothing), victim eligible
(players and hostile mobs, never passive animals) — and makes one decision call, so it never
re-checks behind the decision. Effect, puff, and heal land in `AbilityApplier` (one announce
path, one puff helper); consume moved to `PassiveApplier.consumeStacks`, which reports whether
the stacks signal went stale. Jar deployed; in-game feel (keybinds, farm upkeep, puff size) is
the 05 manual pass.

Triage: stays ready-for-human, narrowed — the attacker-hook / War Cry half is moot (deleted,
spec superseded). The Second Wind half (G keybind, heal fire path) survives and is still
covered by `SecondWindAbilityTest`; only its in-game press remains for a human.
