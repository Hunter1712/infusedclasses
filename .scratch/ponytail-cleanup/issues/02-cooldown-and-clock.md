# 02: Single cooldown query and inline clock

**What to build:** One canonical War Cry cooldown query (fire + remaining-time, zero means ready) with wall-clock passed in at the call site. No separate ready boolean, no clock helper.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Fire plus remaining-time as canonical queries, zero is ready
- [x] Separate ready boolean removed from production
- [x] No dedicated clock helper, time inlined at War Cry call sites
- [x] Relog cooldown reset documented as accepted default

## Comments

CLOSED — already done. `Cooldowns.java:13,23` `useWarCry` + `warCryRemaining`, no `isReady`; `WarCryAbility.java:69,73-76` takes `now`, zero is ready; `InfusedClasses.java:72,77,118` inlines `currentTimeMillis`; `PlayerRuntime.java:17` documents reset; `gradle cleanTest test` green.
