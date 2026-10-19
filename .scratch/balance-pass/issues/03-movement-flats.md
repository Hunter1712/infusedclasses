# 03 — Movement flats and flat copy

Status: done
Spec: `.scratch/balance-pass/spec.md`

## What

Movement stays `ADD_VALUE`; the numbers move to a 0.005 grid and every `%` claim leaves the copy.

## Where

- `Momentum.TIERS` weights `-0.002/-0.004/-0.007` → `-0.005/-0.01/-0.015`.
- `RotateAbility.SCUTUM_WEIGHT` `-0.006` → `-0.005`.
- `ConditionalPassive.fullDesc` renders weights through `amount()`; delete `percent()` (callers: `fullDesc`, `OffloadAbility.desc`).
- `OffloadAbility.desc` renders `+0.01 speed`; Roulette Haste copy is already `+0.02`.
- Update `MomentumTest`/`ConditionalPassiveStacksTest`/`OffloadAbilityTest`/`LegionaryTest` expectations for the flat copy and the new weights.

## Done when

`./gradlew test` green; no `percent(` movement call left; copy in the picker reads `-0.005 speed` etc.
