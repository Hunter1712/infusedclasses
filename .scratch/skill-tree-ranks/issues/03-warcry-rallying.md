# 03: War Cry header and Rallying Roar wiring

**What to build:** The owned War Cry header (presentation only, no spend or state) with Hold the Line's edge drawn up to it, plus Rallying Roar server wiring: Absorption on cast through the existing shout path and the War Cry cooldown parametrized by unlocks (thirty seconds base, twenty with Rallying). Cooldowns still reset on relog.

**Blocked by:** 02 (needs the rules and result type).

**Status:** ready-for-human

- [x] Header renders owned for Warriors with no spend path; Hold the Line edge targets it
- [ ] Rallying cast grants Absorption with the longer window; non-Rallying casts behave exactly as today
- [ ] Cooldown shortens only while Rallying is owned; relog reset behavior unchanged

## Comments

Code in `3c68c71`, effects unit-tested. Header confirmed in screenshots. Remaining: one live War Cry cast with Rallying owned (golden hearts + ~20s recharge) — needs a human in-game.
