# 04: Warrior passives

**What to build:** Warrior players get permanent attribute modifiers: increased max health, armor bonus, +2 sword/axe damage, -10% movement speed. Switching class removes old modifiers first. JUnit tests cover the modifier math.

**Blocked by:** 02

**Status:** ready-for-agent

- [x] Modifiers apply on class set
- [x] Modifiers removed on class switch (no stacking)
- [x] JUnit tests verify expected values

## Comments

Implemented. gradle build green, 8 JUnit tests pass. GUI/keybind need one manual dev-client check.
