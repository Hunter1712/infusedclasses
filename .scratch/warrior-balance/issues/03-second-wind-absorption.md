# 03 — Second Wind absorption rework

Status: done

Spec: `.scratch/warrior-balance/spec.md`

Blocked by: 01

## Task

Rework Second Wind (G, 60s) from flat heal to shield: consume all Bulwark →
absorption `stacks × 1.0 HP` for 10s (max 20 HP = 100% of bar). Name, key, lane,
fizzle-at-zero, and full-spend `consume` semantics all unchanged.

- `SecondWindAbility.java`: result carries absorption HP + 10s window instead of
  heal HP. `healAmount` → absorption-amount decision (naming: implementer's call,
  keep the diff small).
- `AbilityApplier.java:24-36`: apply shield instead of `player.heal()`.
  Absorption granularity (4 HP/level effect steps don't divide 1.0 HP/stack
  evenly — effect-amplifier mapping vs direct amount) to be resolved against the
  1.21.11 jar via `javap` at implementation time.
- `PassiveApplier.consumeSpent` path unchanged — spender still drops stacks +
  armor modifier.

## Acceptance

- Pure JUnit: absorption math at 0/5/20 stacks (0/5/20 HP); fizzle arms nothing;
  consume clears stacks and armor plan.
- Manual: build stacks, press G at low HP — yellow hearts, live HP stays low,
  Berserk bonus stays on.
- `gradle test` green.

## Comments

None yet.
