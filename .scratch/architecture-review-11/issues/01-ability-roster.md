# 01 — One ability roster table, five consumers

Status: done

## Context

An ability's word is restated in `StatusHud`'s constants (a second copy of the ability
module's `NAME`); its word and lane read are restated again in `ClassInfo.LANES`; its slot
bind is re-checked by id in `BindMorph`. Round-11 candidate 1.

## Deliverable

- New `AbilityRoster` (main, pure): one `Row(ability, name, laneRemaining, signature,
  ultimate)` per ability, in the current `ClassInfo.LANES` order (Rush, Rend, Rotate,
  Aequilibrium, Roulette, All In); `all()` and `byAbility(id)`.
- `ClassInfo` deletes `LANES` and takes the runtime state read; lane lines walk roster
  rows filtered by `def.hasAbility`.
- `StatusHud` deletes its six ability-word constants; rows read roster names. Stance
  words read `Stance.displayName()`, the Momentum word the passive's own name, Streak
  `CritRule.STREAK`.
- `BindMorph` deletes its id checks: `signatureFor`/`ultimateFor` walk the class's
  actives through roster rows (first non-null target wins) with the classless fallback
  unchanged. The ultimate still keys on the ROTATE row (ADR-0009.3).

## Done when

- `gradle test` green; BindMorphTest unchanged in spirit; ClassInfoTest/StatusHudTest
  read roster words; a completeness test pins that every `ClassRegistry` active id has
  a roster row.
