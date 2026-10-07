# 01: Juggernaut/Vigor full rename + NBT migration (no behaviour change)

Spec: `.scratch/juggernaut-rush-rend/spec.md`

**Blocked by:** None (can start immediately).

**Status:** done

## Task

Rename only. Behaviour (Bloodlust lifesteal, Second Wind shield, Bulwark
20x0.5, Fury per-steps, lanes) is byte-identical after this ticket:

- `ClassRegistry.java:19`: `BERSERKER` → `JUGGERNAUT`, id
  `infusedclasses:berserker` → `infusedclasses:juggernaut`, display
  `Berserker` → `Juggernaut`. Colour/tagline unchanged.
- `ClassRegistry.java:50-61`: `Fury Damage`/`Fury Speed`, group `Fury`,
  modifier ids `fury_damage`/`fury_speed` → `Vigor Damage`/`Vigor Speed`,
  group `Vigor`, ids `vigor_damage`/`vigor_speed`. Per-steps `0.3`/`0.05`
  unchanged. Maths still buckets on missing HP here — ticket 02 inverts it.
- `PlayerClasses`: `readId` maps stored `infusedclasses:berserker` forward
  to `infusedclasses:juggernaut` (extend the existing
  `warrior` → `berserker` chain, don't break it). No wipes.
- Fix every `BERSERKER`/Berserker/Fury caller in `src/main/java` +
  `src/test/java` (ability javadoc, `PassiveApplier` comments,
  `ClassPresentationTest`, `SecondWindAbilityTest#berserker()` /
  `ConditionalPassiveStacksTest#berserker()` helper names are
  implementer's call but assertions must pass).
- `GLOSSARY.md`: class entry Berserker→Juggernaut, passive entry
  Fury→Vigor. Bloodlust/Second Wind entries untouched (ticket 04 owns
  Rush/Rend docs).

## Acceptance

- `gradle test` green with zero behaviour-code changes (rename + migration
  only; `BloodlustAbility`/`SecondWindAbility` files still exist, still wired).
- New unit: stored berserker id loads as Juggernaut definition; stored
  juggernaut id loads; stored warrior id still loads forward; unknown ids
  still inert.

## Comments

None yet.
