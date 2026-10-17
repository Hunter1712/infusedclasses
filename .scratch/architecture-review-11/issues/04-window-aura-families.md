# 04 — One window-aura family list in PassiveApplier

Status: done

## Context

`PassiveApplier` enumerates the window aura families five times: `applyStance`,
`applyRoulette`, `removeStanceModifiers`, `removeRouletteModifiers`, `allModifiers`,
plus the two marker branches in `onClassChanged` — and `RushApplier.tick` is a sixth
mechanism for one modifier. Round-11 candidate 4.

## Deliverable

- One `FAMILIES` list: marker ability, every modifier the family can write, and
  `wanted(state)` (the pure half, package-private and tested without a player).
- `apply` reconciles each family once after the stack/Vigor passes: a held marker
  reconciles write-wanted/remove-rest; an absent marker removes everything the family
  can write (so `remove*Modifiers` and `allModifiers` die).
- `onClassChanged` drops families whose marker the new class lacks.
- `RushApplier.tick` folds into a Rush family (speed while live), so the speed write
  lives with the other window auras; `RushApplier` keeps the cleanse and announce.

## Done when

- The top half of `PassiveApplier` has no per-mechanism branch; behaviour is
  byte-identical (`gradle compileJava` + family tests green).
