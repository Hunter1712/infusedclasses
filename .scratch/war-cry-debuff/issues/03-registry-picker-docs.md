# 03: Warrior registry, picker copy, docs, and verification

**What to build:** Warrior carries the shout and the picker describes it honestly.

**Blocked by:** 01.

**Status:** done

- [x] `WAR_CRY_ABILITY` id; Warrior `activeAbilities` carries both ids (War Cry first), `actives` gains the War Cry entry with copy built from the 01 constants; Second Wind entry and numbers unchanged.
- [x] Presentation tests assert both actives on the card plus the tooltip line with honest numbers; locked classes unchanged.
- [x] Glossary regains War Cry (debuff meaning), Cooldown/Active/Class entries cover two actives and two lanes.
- [x] `gradle test` green plus `gradle deployClient`; in-game feel (radius, effect icons, R/G independence) is manual.

## Comments

95 tests green. Jar live via `deployClient`. Remaining human pass: shout in a pack, confirm Weakness/Slowness icons and 16-block reach, R/G independence.
