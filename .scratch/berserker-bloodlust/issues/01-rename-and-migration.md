# 01: Berserker/Fury full rename + NBT migration (no behaviour change)

Spec: `.scratch/berserker-bloodlust/spec.md`

**Blocked by:** None (can start immediately).

**Status:** ready-for-human

## Task

Rename only. Behaviour (War Cry debuff, +5 builder, Bulwark 20×0.5, Fury
per-steps, lanes) is byte-identical after this ticket:

- `ClassRegistry.java:19,38-39`: `WARRIOR` → `BERSERKER`, id
  `infusedclasses:warrior` → `infusedclasses:berserker`, display `Warrior` →
  `Berserker`. Colour/tagline unchanged.
- `ClassRegistry.java:50-61`: `Berserk Damage`/`Berserk Speed`, group `Berserk`,
  modifier ids `berserk_damage`/`berserk_speed` → `Fury Damage`/`Fury Speed`,
  group `Fury`, ids `fury_damage`/`fury_speed`. Per-steps `0.3`/`0.05`
  unchanged.
- `PlayerClasses`: `readId` maps stored `infusedclasses:warrior` forward to
  `infusedclasses:berserker`. No wipes.
- Fix every `WARRIOR`/Warrior caller in `src/main/java` + `src/test/java`
  (`WarCryAbility`, `SecondWindAbility` javadoc, `PassiveApplier` comments,
  `ClassPresentationTest`, `WarCryAbilityTest#warrior()` helper names are
  implementer's call but assertions must pass).
- `GLOSSARY.md`: class entry Warrior→Berserker, passive entry Berserk→Fury.
  War Cry entries untouched (ticket 04 owns Bloodlust docs).

## Acceptance

- `gradle test` green with zero behaviour-code changes (rename + migration
  only; `WarCryAbility`/`WarCryApplier` files still exist, still wired).
- New unit: stored warrior id loads as Berserker definition; stored berserker
  id loads; unknown ids still inert.

## Comments

None yet.
