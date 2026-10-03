# 06: /class set command

**What to build:** Admin command sets a player's class, removes previous class modifiers, applies new ones, and updates NBT.

**Blocked by:** 02, 04

**Status:** ready-for-agent

- [x] Command parses player + class id
- [x] Old modifiers removed, new applied
- [x] NBT updated; GUI not reshown on next join

## Comments

Implemented. gradle build green, 8 JUnit tests pass. GUI/keybind need one manual dev-client check.
