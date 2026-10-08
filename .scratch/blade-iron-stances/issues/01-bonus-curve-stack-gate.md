# 01: Universal charged rule — BonusCurve + full-swing stack gate (pure + hook)

Spec: `.scratch/blade-iron-stances/spec.md`

**Blocked by:** none.

**Status:** done

## Task

The melee-wide spam fix. No Legionary, no stances, no windows:

- New pure `BonusCurve`: `bonusFor(baseHp, charge) = baseHp x charge x charge`
  (1.0 full, 0.25 half, ~0 spam, clamp charge 0–1, base <= 0 reads 0). Applied
  to mod hook bonus damage only. Base weapon damage never touched.
- Attacker branch in `ServerEvents` (existing melee check): read
  `attackStrengthScale` once per swing (exact signature confirmed via `javap`
  against the 1.21.11 merged jar at implementation time), pass it to
  `PlayerRuntime.onTrigger` (signature gains the charge), feed conditional
  triggers only at `charge == 1.0`. Spam earns zero; unknown triggers stay
  inert. Route the `Rend` empower through `BonusCurve` at the same call site,
  so every melee class shares one curve with no per-class copies.
- `Juggernaut Momentum` grace 4s -> 5s (`ClassRegistry.java`) to cover the
  swing timer under the new gate. Nothing else on Juggernaut.

## Acceptance

- Pure JUnit: `bonusFor` at charges 0.0/0.5/1.0 reads 0/0.25x/1.0x; charge > 1
  clamps to 1.0; base <= 0 reads 0; spam feeds zero stacks, full feeds one;
  grace re-stamp; `Rend` bonus at half charge reads quarter.
- `gradle test` green. No registry additions, no applier changes beyond the
  one call site.

## Comments

Done: `BonusCurve` pure + `onTrigger` charge overload (old callers read as full),
attacker branch gates stacks at 1.0 with `getAttackStrengthScale(0.0F)` (javap-verified),
Rend empower routed through the curve, Momentum grace 4s -> 5s. `gradle test` green.
