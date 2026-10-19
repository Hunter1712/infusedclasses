# 01 — Full swing module: delete the curve, move the gate

Status: done
Spec: `.scratch/balance-pass/spec.md`

## What

Delete `BonusCurve` (`bonusFor` and its tests) and add `FullSwing.isFull(charge)` in its place. The gate is the only surviving rule: exact `charge >= 1.0F`.

## Where

- New `src/main/java/com/hunter1712/infusedclasses/FullSwing.java` — `public static boolean isFull(float charge)`, same exactness and javadoc rationale (loosen only with a measured charge histogram).
- Delete `BonusCurve.java`; delete `BonusCurveTest`'s curve tests, rename the file/class to `FullSwingTest` keeping the gate tests.
- Callers rename to `FullSwing.isFull(charge)`: `PassiveStacks.onTrigger`, `CritRoll.roll`, `MeleePlan.planHit` (bump and Onslaught price), `ServerEvents.settleAttackerHit` (roll draw).

## Done when

`./gradlew test` green; no `BonusCurve` reference anywhere; GLOSSARY "Full swing" anchor updated in ticket 04.
