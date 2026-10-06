# 01: War Cry extension decision as pure data

**What to build:** A pure module answering one question: given a melee hit, should War Cry's Strength window grow. Takes the RNG roll in `[0,1)` so randomness stays in the adapter and JUnit asserts without it. Fires only when the attacker is a Warrior, the swing is fully charged, the live Strength instance exists, and the roll is under the rate. No cap, no internal cooldown, no per-swing dedup; the charge gate is the only throttle.

**Blocked by:** None (can start immediately)

**Status:** wontfix

- [x] New pure decision, e.g. `WarCryExtension.extendRoll(hasEffect, remainingTicks, roll)` or equivalent shape, with rate `0.25` and extension `+40 ticks (2s)` as named constants next to the decision
- [x] Fires only when charged plus effect live plus `roll < 0.25`; uncharged swings, missing/cleared Strength (milk/death), non-Warrior attackers, and rolls at/above rate all yield no extension
- [x] Arithmetic is `remaining + 40` with no clamp; sustained combat can hold Strength II indefinitely
- [x] Multi-victim independence is N calls (one sweep into N mobs is N rolls), asserted at the decision seam with no player, level, or graphics object constructed
- [x] Source of truth stays the live `MobEffectInstance` duration, never a runtime timestamp
- [x] Existing `WarCryAbility`, `Cooldowns`, and stack/decay tests pass unchanged

## Comments

Implemented as `WarCryExtension` (`PROC_RATE`, `EXTEND_TICKS`) returning the new duration or 0;
signature carries `isWarrior`/`fullyCharged` in as well so the adapter never re-checks behind the
decision. `WarCryExtensionTest`: 4 tests green, full suite 110 pass, jar deployed via `gradle deployClient`.

Triage: wontfix — War Cry and its extension were deleted, spec superseded; the in-game
verification never happened and is now moot.
