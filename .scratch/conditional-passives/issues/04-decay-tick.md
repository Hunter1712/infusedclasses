# 04: Decay tick

**What to build:** After being un-hit for 5 seconds, stacks decay by 1 per second.

**Blocked by:** 03.

**Status:** done

- [x] Un-hit grace period is 5 seconds.
- [x] After grace, stacks decrease by 1 per second.
- [x] New hit resets the grace timer.

## Answer

Implemented in `PlayerClasses.tickDecay()` with testable overload. Unit tests in `ConditionalPassiveStacksTest`:
- `noDecayWithinGracePeriod`
- `decayStartsAfterGracePeriod`
- `decayMultipleStacksAfterExtendedTime`
- `newTriggerResetsGraceTimer`

Server tick hook in `InfusedClasses` calls `tickDecay()` every tick.