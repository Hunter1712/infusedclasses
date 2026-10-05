# 03: Client stacks store and countdown

**What to build:** Client-side store for the last synced entries per passive that ticks the displayed countdown down locally between server messages. No drawing here.

**Blocked by:** 02.

**Status:** done

- [x] Receiver stores entries by passive name on the client thread; empty signal clears them; class confirm does not wipe mid-fight state.
- [x] Seconds-left interpolation derives from stored millis-until-decay minus elapsed client time, floored at zero; stale entries past grace read as zero.
- [x] Unit test with a fake clock: countdown ticks down without new packets, new server entry re-stamps it, zero-stack entry hides the row.
- [x] No `DrawContext` or screen code in this module.

## Answer

`client/ClientPassiveStacks` (static store: `setAll`, `stacksFor` with local decay, `millisUntilDecay`) plus `ClientPassiveStacksTest` (4 tests, fake clock passed as `now`). Confirm handler untouched, so mid-fight state survives it. `gradle test` green.
