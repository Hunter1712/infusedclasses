# 02: Class registry + NBT persistence

**What to build:** A class registry mapping class id to its definition (display name, locked flag, passives, active), and saving the chosen class id on the player via NBT. JUnit tests cover serialization and registry lookup.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] Registry contains Warrior definition; other planned classes present and marked locked
- [x] Player class id persists across save/load
- [x] JUnit tests: round-trip serialize, unknown id handling

## Comments

Implemented. gradle build green, 8 JUnit tests pass. GUI/keybind need one manual dev-client check.
