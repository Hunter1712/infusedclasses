# 02: Second Wind heal decision plus independent 60s cooldown

**What to build:** A pure Second Wind module mirroring the `WarCryAbility` trigger/fire gate-and-arm shape, plus its own 60s cooldown lane that advances independently of War Cry's 30s. Heal math is `stacks * 1.0 HP` (half heart per Bulwark stack, max 10 HP at 10 stacks). Zero stacks fizzles with a `No Bulwark` line and arms nothing. Spender semantics live in the decision: consume all Bulwark stacks on fire, so the adapter clears stacks, timestamps, armor, and resends the stacks signal.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

- [x] New pure decision, e.g. `SecondWindAbility`, with `healAmount(stacks) = stacks * 1.0 HP` asserted at 0/5/10 stacks
- [x] Zero stacks yields a fizzle result (chat line `No Bulwark`, no heal, no consume); wrong-class and on-cooldown presses likewise leave cooldowns untouched
- [x] `Cooldowns` gains an independent heal lane (own field plus `SECOND_WIND_MS = 60_000L`); `PlayerRuntime` exposes it through verbs (e.g. `tryUseSecondWind` / `secondWindRemaining`) so callers stop reaching through the mutable object
- [x] Fire path is gate-and-arm together: deciding fires only when Warrior plus off-cooldown plus stacks > 0, and arming happens only on fire, matching the existing `trigger`/`fire` shape
- [x] Consume plan is part of the result: clear stacks plus last-trigger timestamps, remove the armor modifier via the passive applier path, resend the stacks signal so no stale row sticks around
- [x] Cooldown machine covered in plain JUnit: War Cry 30s and heal 60s advance independently; rejected presses never arm; class switch and respawn reset both
- [x] No shared cooldown and no `refund`-style verb sharing between `R` and `G`

## Comments

Implemented as `SecondWindAbility` (kinds FIRED/WRONG_CLASS/ON_COOLDOWN/NO_BULWARK, consume flag in
the result, announcement reuses `WarCryAbility.ChatLine`). Fizzle is checked before the cooldown
read so a 0-stack fat-finger reports `No Bulwark` either way. `SecondWindAbilityTest`: 6 tests
green, full suite 110 pass. Fixed one self-found bug on the way: `onCooldown` dropped the
remaining time (hardcoded 0); the independence test caught it.
