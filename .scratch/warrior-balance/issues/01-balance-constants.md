# 01 — Balance constants

Status: done

Spec: `.scratch/warrior-balance/spec.md`

## Task

Apply the grilled number changes, constants only (no behaviour rework):

- `ClassRegistry.java:60`: Warrior `maxHealthBonus` 20.0 → 0.0.
- `ClassRegistry.java:41-48`: Bulwark `ConditionalPassive` 10 × 1.0 → 20 × 0.5
  (grace stays 5).
- `ClassRegistry.java:50-59`: Berserk per-steps 0.4 / 0.08 → 0.3 / 0.05.
- `Cooldowns.java:8`: `WAR_CRY_MS` 60s → 20s (`SECOND_WIND_MS` untouched).
- `WarCryAbility.java:19`: `EFFECT_TICKS` 160 → 200 (10s).

## Acceptance

- Pure JUnit: Bulwark cap/grace math at 0/5/20/21 stacks; Berserk bucket × new
  per-steps (half HP ≈ +1.5/+0.25, near death ≈ +2.1–3.0); cooldown independence
  (20s War Cry / 60s Second Wind advance separately, rejects never arm).
- `gradle test` green. No behaviour-code changes in this ticket.

## Comments

None yet.
