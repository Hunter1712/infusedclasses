# 01: Class XP, levels, points + persistence

**What to build:** Killing mobs earns class XP with melee affinity, levels 1-20 grant skill points, and progress survives death and relog.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Sword and axe kills grant bonus XP, other kills grant base XP
- [ ] Level curve is fast early and slow to 20, 1 point per level
- [ ] XP, level, and unspent points persist across death, respawn, and relog
- [ ] JUnit covers curve math, affinity bonus, and serialization round-trip
