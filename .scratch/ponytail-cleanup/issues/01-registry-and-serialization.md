# 01: Single registry lookup and serialization path

**What to build:** One nullable class lookup and one string id round-trip used by registry, persistence, picker, and tests. Malformed ids yield no class.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Single nullable registry lookup, no Optional variant
- [x] Single string read/write pair for class id persistence
- [x] Record-level NBT helpers removed
- [x] Malformed/unknown ids yield no class through one path

## Comments

CLOSED — already done. `ClassRegistry.java:93` nullable `byId`, no Optional lookup; `PlayerClasses.java:26,30` `writeId/readId` via `Identifier.tryParse`, `ClassDefinition.java:8-18` bare record; `PlayerMixin.java:27,32` uses only that path; `gradle cleanTest test` green.
