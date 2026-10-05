# 08: Card shows passive short description

**What to build:** Unlocked class card shows each passive as name plus short description; empty passives show `No passives yet`. Copy comes from passive fields so it cannot drift.

**Blocked by:** 01.

**Status:** done

- [x] Card shows one name line and one short-desc line per passive
- [x] Empty classes show `No passives yet`
- [x] Tooltip still includes full description + max stacks

## Comments

Gap vs spec `:34` + stories 7-8: `ClassPresentation.java:88-93` renders name only and skips empty; `ConditionalPassive.java:19-28` has no `shortDesc` field.

Implemented as generated `shortDesc()` (`"+1 armor per stack"`) next to `fullDesc()` — no stored field. `cardLinesFor()` emits name + desc as two `PASSIVE` lines; empty emits header + `No passives yet`. Tooltip untouched. `gradle test` green.
