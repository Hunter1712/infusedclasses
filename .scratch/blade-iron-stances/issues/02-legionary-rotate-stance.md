# 02: Legionary registry + Rotate + stance runtime and auras (pure + apply)

Spec: `.scratch/blade-iron-stances/spec.md`

**Blocked by:** none (merges with 01 in the attacker branch; 01 lands first).

**Status:** done

## Task

The class and its toggle. No `G` windows yet (`Impetus`/`Testudo` are ticket 03):

- `ClassRegistry`: new `LEGIONARY` (`infusedclasses:legionary`, display
  `Legionary`, playable, accent bronze `0xFFB87333`, tagline `Sword and
  shield`), actives `[rotate, impetus, testudo]` with copy built from
  constants, ids `infusedclasses:rotate`, `infusedclasses:impetus`,
  `infusedclasses:testudo`. New attribute keys `MOVEMENT_SPEED` and
  `ARMOR_TOUGHNESS` next to the existing keys. No migration (new id);
  Ranger/Mage untouched.
- New `RotateAbility`: `NAME` "Rotate", `ROTATE_MS` 8_000, kinds
  `FIRED`/`WRONG_CLASS`/`ON_COOLDOWN`; `trigger` pure read-only (class gate
  -> cooldown gate, never fizzles on stance); `fire` gates and flips the
  runtime stance together. FIRED announces the new stance (`Gladius!` red /
  `Scutum!` gray), no cast word. Rejects arm nothing.
- `PlayerRuntime`/`PlayerRuntimes`: holds `stance` (default `SCUTUM`) plus the
  `rotate` lane; respawn/switch/logout clears it and resets stance to
  `SCUTUM`. Choice stays in `PlayerClasses`; stance is transient per ADR-0002.
- `PassiveApplier`: applies only the current stance's modifiers and removes
  the other's, write-only-on-change like the Vigor tick. `Gladius`:
  `attack_damage` +2.0 (`infusedclasses:gladius_damage`), `attack_speed` +0.2
  (`infusedclasses:gladius_alacrity`). `Scutum`: `armor` +6
  (`infusedclasses:scutum_plate`), `armor_toughness` +1
  (`infusedclasses:scutum_toughness`), `movement_speed` -8%
  (`infusedclasses:scutum_weight`). Stance deal/take mults (`Gladius` take
  x1.2, `Scutum` deal x0.8) multiply in the `ServerEvents` melee branches via
  pure `dealMult`/`takeMult` helpers. No negative attribute modifiers.
- `R` keeps its bind as the `rotate` intent (bind-survival rule).

## Acceptance

- Pure JUnit: Rotate fires in any stance, blocks only on cooldown/wrong-class;
  fire flips stance and arms only its own lane; `dealMult`/`takeMult` stance
  matrix (Gladius take x1.2, Scutum deal x0.8); stance reset to `SCUTUM` on
  clear; applier writes on change only, removes the left stance.
- `gradle test` green. No `G` behavior in this ticket.

## Comments

Done: `LEGIONARY` + rotate/impetus/testudo ids, `RotateAbility` (8s lane, stance
flip, `dealMult`/`takeMult`), stance on runtime default `SCUTUM`, applier auras
write-only-on-change, `R` bind reuses Rush payload per class. `gradle test` green.
