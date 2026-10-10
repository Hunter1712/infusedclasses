# 04: Wire settle + copy + docs + verify

Spec: `.scratch/legionary-aequilibrium/spec.md`

**Blocked by:** 01, 02, 03.

**Status:** done

## Task

Land the combination and prove it:

- Hit settle: Impetus flip bonus spends exactly once through the same
  peek/consume pairing the plan saw (mirror the Rush/Rend settle order), so
  empty hits never touch a window. Testudo flip mult settles through
  `DamageSettle` with the stance mults. Combined peak (base x switch +
  Aequilibrium) is intentional.
- Picker copy: `Rotate` switch lines, rewritten `Impetus`/`Testudo` flip
  descs, new `Aequilibrium` entry, all generated from constants. Card:
  `Passives` header plus stance lines as before, `Active` header with one
  line per active (`[rotate, impetus, testudo, aequilibrium]`).
- Docs: `GLOSSARY.md` retunes Gladius/Scutum/Rotate entries, rewrites
  Impetus/Testudo as flip buffs, adds Aequilibrium, and one ADR for the
  stance-to-switch plus ultimate-swap decisions.
- Update `LegionaryTest` (numbers, matrix, copy, lane/window), `MeleePlanTest`
  (deal/take matrix + switch windows), `BindMorphTest` (enum rename, no
  morph). `Cooldowns` untouched (lane reused).

## Acceptance

- Combined-peak test: base x switch + Aequilibrium, no override; flip +
  window clear on reset; no oath-window text anywhere.
- `gradle test` green. Manual: `gradle deployClient`, `/class select`,
  confirm 2-hit TTK feel and the 1.5s catch window in game.

## Comments

Landed: Impetus spends once through the plan/settle peek-consume pair, Testudo settles
through `DamageSettle` with the stance mults, combined-peak test pins no override,
glossary retuned plus ADR-0010, `gradle test` green (138). `Cooldowns` untouched.
Deviation: `KNOCKBACK_RESISTANCE` key removed (brace deletion left it unused).
Manual in-game verify (`deployClient`, TTK feel) still open.
