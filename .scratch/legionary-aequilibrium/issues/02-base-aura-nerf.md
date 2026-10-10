# 02: Base aura nerf + speed/weight rebalance

Spec: `.scratch/legionary-aequilibrium/spec.md`

**Blocked by:** none (merges with 01 in `MeleePlan`; 01 lands first).

**Status:** done

## Task

Halve the always-on stats in `RotateAbility`, numbers only, shape unchanged:

- `GLADIUS_DAMAGE` 2.0 -> 1.0, `GLADIUS_TAKEN_MULT` 1.2 -> 1.1,
  `GLADIUS_SPEED` 0.2 -> 0.3 (pays back part of the damage cut as rate).
- `SCUTUM_ARMOR` 6.0 -> 3.0, `SCUTUM_DEAL_MULT` 0.8 -> 0.9,
  `SCUTUM_WEIGHT` -0.008 -> -0.006. `SCUTUM_TOUGHNESS` 1.0 unchanged.
- New constants `SWITCH_DEAL_MULT` 1.10, `SWITCH_DEAL_MS` 5_000,
  `SWITCH_TAKE_MULT` 0.85, `SWITCH_TAKE_MS` 1_500 next to the aura numbers.
- `auraFor` / `dealMult` / `takeMult` keep shape. `StanceApplier` untouched
  (iterates the same aura lists). `Rotate` picker desc gains the switch
  lines, built from the constants.

## Acceptance

- Pure JUnit: retuned `dealMult`/`takeMult` matrix (take 1.1 Gladius, deal 0.9
  Scutum); aura lists carry the new amounts; picker copy reflects live
  numbers.
- `gradle test` green.

## Comments

Landed: aura numbers retuned in `RotateAbility`, `Rotate` desc gains the switch lines built
from the Impetus/Testudo switch constants. `SWITCH_*` live on the flip-buff abilities
per ticket 01, not duplicated next to the auras.
