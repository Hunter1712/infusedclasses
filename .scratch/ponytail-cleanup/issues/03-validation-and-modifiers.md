# 03: Single validation and scoped modifiers

**What to build:** Class select validates once in the player state module (unknown/locked rejected once), join resolves state once, modifier removal stays internal, attribute lookup covers only used attributes.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Select flow validates once, no redundant pre-lookup
- [x] Join flow has no double state resolve
- [x] Modifier-removal helper scoped internal to its module
- [x] Attribute lookup covers only attributes a class uses
- [x] Unknown/locked class rejected once, retry unchanged

## Comments

CLOSED — already done. `PlayerClasses.java:42-49` single validation, `InfusedClasses.java:99-107` delegates directly; `InfusedClasses.java:40-45` single resolve; `PassiveApplier.java:55,62-67` private remove + generic lookup, no `attack_damage` branch; `ClassCommands.java:34-38` guards; `gradle cleanTest test` green.
