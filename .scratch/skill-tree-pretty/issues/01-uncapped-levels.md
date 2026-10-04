# 01: Uncapped levels with flattened post-20 cost

**What to build:** Leveling that never hard-caps: the existing exponential XP cost runs through level 20 unchanged, then flattens to a fixed per-level ceiling so play keeps earning one skill point per level indefinitely.

**Blocked by:** None (can start immediately).

**Status:** wontfix

## Comments

SUPERSEDED — implemented in `1fbfc1c`/`e7f03d5`, then the design was replaced by the ranked model in `skill-tree-ranks` ( fifteen one-shot nodes folded into Vigor/Bulwark/Hide rank lines; XP base raised 15 to 22 there). Do not implement from this file; its surviving intent (uncapped levels, flat post-20 ceiling) lives on in ranks issue 01.

- [ ] Earning XP past level 20 grants further levels at one point per level with the flattened cost
- [ ] Levels 1-20 costs are unchanged (fast early, slow late)
- [ ] Pure-logic tests cover the flatten ceiling, uncapped point accrual, and progress round-trip with high levels
- [ ] Manual check via testing command: grant XP past old cap and confirm level/points climb with toast and sound intact
