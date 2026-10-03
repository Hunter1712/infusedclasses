# 03: First-join class GUI

**What to build:** On first join (no class in NBT), the client opens a class-selection GUI listing all classes; Warrior selectable, others greyed. Selection is sent to the server, saved to NBT, and the GUI closes. Returning players never see it.

**Blocked by:** 02

**Status:** ready-for-agent

- [x] New player sees GUI on join
- [x] Locked classes not selectable
- [x] Warrior choice persists; GUI does not reappear on rejoin

## Comments

Implemented. gradle build green, 8 JUnit tests pass. GUI/keybind need one manual dev-client check.
