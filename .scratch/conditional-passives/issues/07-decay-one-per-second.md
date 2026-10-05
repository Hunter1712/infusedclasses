# 07: Decay ticks one stack per second

**What to build:** After 5s without a hit, Bulwark loses exactly 1 stack per second; a new hit resets the grace timer. No acceleration over time.

**Blocked by:** 04.

**Status:** done

- [x] Tick at +6s drops 1 stack, tick at +7s drops 1 more (not 2)
- [x] New hit resets grace timer
- [x] Unit tests cover multi-tick decay without acceleration

## Answer

Fixed double-count in `PlayerRuntime.tickDecay()`: after decaying, `lastTriggerTime` advances by removed stacks so each overdue second counts once. Added `sequentialTicksDecayOneStackPerSecondWithoutAcceleration`. `gradle test` green.
