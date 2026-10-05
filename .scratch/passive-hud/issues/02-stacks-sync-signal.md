# 02: Stacks sync signal

**What to build:** New server-to-client signal (same family as the picker signal and pick confirm) carrying per-passive entries: passive name, stacks, per-stack value, grace seconds, and millis-until-decay at send time. Payload record owns its `Type` and codec; `ClassPackets` stays the `MOD_ID` holder.

**Blocked by:** 01.

**Status:** done

- [x] S2C payload with per-passive entries round-trips through its codec (empty list means clear).
- [x] Entry-building decision is pure: from class definition plus transient runtime plus now, emit one entry per passive (including zero-stack entries that clear the row); classless yields empty.
- [x] Unit test: entries emitted only when the decay moved set is non-empty, plus on class change and respawn clear; millis-until-decay matches the runtime's last-trigger clock.
- [x] No armor attribute write changes; `PassiveEffect` plus passive applier stay the sole modifier path.

## Answer

`PassiveStacksPayload` (entries plus `entriesFor`, fed by a new `PlayerRuntime.lastTrigger` getter) plus `PassiveStacksSyncTest` (3 tests). `ProtocolTest` channel count 4 to 5. Emit gating lives in the server entry: hit path always re-sends (re-stamps grace), tick path sends only when stacks changed, class-switch sends both sides, respawn sends the fresh zeros. `gradle test` green.
