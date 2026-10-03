# 05: War Cry active ability

**What to build:** Warrior presses a keybind; server validates class and cooldown, applies Strength II for 8s to the player, starts a 30s cooldown, shows particles/sound and cooldown feedback. JUnit tests cover the cooldown state machine.

**Blocked by:** 02, 04

**Status:** ready-for-agent

- [x] Keybind triggers ability server-side
- [x] Strength II 8s applied, 30s cooldown enforced
- [x] Non-warrior or cooled-down press does nothing
- [x] JUnit tests: cooldown state machine

## Comments

Implemented. gradle build green, 8 JUnit tests pass. GUI/keybind need one manual dev-client check.
