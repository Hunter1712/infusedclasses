# 04: Docs and picker polish

**What to build:** Mod description, toolchain docs, and modifier-mechanism wording match implementation (War Cry is the damage tool, no flat melee bonus). Picker computes layout count once per pass.

**Blocked by:** 01, 02, 03.

**Status:** done

- [x] Mod description matches Warrior behavior, no promised melee bonus
- [x] Documented Java toolchain matches build
- [x] Modifier mechanism wording matches platform API
- [x] Picker layout count not queried per frame

## Comments

CLOSED — already done. `src/main/resources/fabric.mod.json:6` Bulwark + War Cry copy; `build.gradle:73-74` Java 21, `fabric.mod.json:25` `>=21`; identifier-namespacing wording in `classes/spec.md:39`; `ClassSelectScreen.java:38` single `copyOf` per layout; `gradle cleanTest test` green.
