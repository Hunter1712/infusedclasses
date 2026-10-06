# 02: War Cry intent, keybind, receiver, and applier

**What to build:** Client-to-server shout path and the adapter that lands it on hostiles.

**Blocked by:** 01.

**Status:** done

- [x] `WarCryPayload` unit intent on `infusedclasses:war_cry`, registered alongside the Second Wind one.
- [x] `R` keybind in the client plus `en_us.json` entry, sent while consumed each client tick; receiver fires the 01 decision.
- [x] `WarCryApplier.apply(player, result)`: announces through the shared path (widened, not duplicated), scans the inflated bounding box for alive `Monster`s, writes each carried effect; nothing spent, zero-stack-style fizzle impossible (no cost).
- [x] Rejected presses (wrong class, on cooldown) leave the lane untouched.

## Comments

One javap catch: no class-based scan overload exists in this mapping, so the adapter uses `getEntities(except, box, predicate)` with an `instanceof Monster` predicate plus a compiler-forced re-check for the cast. `AbilityApplier.announce` widened private→package; Second Wind path untouched.

Review follow-up: holders now resolve once per shout (not per mob), and the `Debuff`
record collapsed to bare ids since ticks/amplifier are shared constants.
