# 01: Uncapped levels with flattened post-20 cost

**What to build:** Leveling that never hard-caps: the existing exponential XP cost runs through level 20 unchanged, then flattens to a fixed per-level ceiling so play keeps earning one skill point per level indefinitely.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Earning XP past level 20 grants further levels at one point per level with the flattened cost
- [ ] Levels 1-20 costs are unchanged (fast early, slow late)
- [ ] Pure-logic tests cover the flatten ceiling, uncapped point accrual, and progress round-trip with high levels
- [ ] Manual check via testing command: grant XP past old cap and confirm level/points climb with toast and sound intact
