# 02: Vigor bucket + Rush/Rend decisions, lanes, windows (pure)

Spec: `.scratch/juggernaut-rush-rend/spec.md`

**Blocked by:** 01 (needs the Juggernaut ids).

**Status:** done

## Task

Pure decision modules plus the HP-bucket inversion. No wiring, no appliers:

- `HpScaledPassive`: add `bucketForCurrent(currentHp, maxHp)` (mirror of
  `bucket`, floor(current * STEPS / max), clamp 0–10, degenerate max (max ≤ 0)
  reads 0). Keep or delete missing-HP `bucket` with its callers — one or the
  other, no dead helper. `fullDesc` gains current-HP wording for the Vigor
  group.
- `ConditionalPassive`: add `ON_HIT_DEALT = "on_hit_dealt"` next to
  `ON_HIT_TAKEN`. `PlayerRuntime.onTrigger` already generic, no change.
- `ClassRegistry`: `Bulwark` (`on_hit_taken`, 20 x 0.5 armor, grace 5) →
  `Momentum` (`on_hit_dealt`, 10 x 1.0 armor, grace 4,
  `infusedclasses:momentum_armor`).
- New `RushAbility`: `NAME` "Rush", `SPEED_TICKS` 80, speed amplifier 0,
  `BONUS_STACKS` 3, `RUSH_MS` 25_000, kinds FIRED/WRONG_CLASS/ON_COOLDOWN;
  `trigger(def, cooldowns, now)` gates class → cooldown (no HP/stack gate);
  `fire` gates and arms together; wind-burst announcement line.
- New `RendAbility`: `NAME` "Rend", `BASE_HP` 6.0F, `HP_PER_STACK` 1.0F,
  `WINDOW_TICKS` 120 (6s), `REND_MS` 60_000, kinds
  FIRED/WRONG_CLASS/ON_COOLDOWN/NO_MOMENTUM; `empowerFor(stacks)`
  pure (`stacks <= 0 -> 0`, else `6 + stacks`, no target term);
  `momentumOf(def)` trigger-found like
  `SecondWindAbility.bulwarkOf`; `trigger`/`fire` gate-and-arm shape with
  consume flag in result, empower snapshot stamped on the runtime next to the lane.
  Sound/effect ids ride on the decisions (`SPEED_EFFECT`, `CAST_SOUND` as
  `Identifier`s per ADR-0001); adapters resolve holders.
- `Cooldowns`: `BLOODLUST_MS`/`SECOND_WIND_MS` → `RUSH_MS` 25_000 /
  `REND_MS` 60_000 with `useRush`/`rushRemaining`, `useRend`/`rendRemaining`;
  `reset` readies both. `PlayerRuntime` exposes lanes plus Rush window
  (`rushPressedAt`/`isRushLive`/`consumeRushBonus` one-shot; respawn/switch
  clears with everything else).
- Delete `BloodlustAbility.java` + `SecondWindAbility.java` and their tests
  only here, replaced by `RushAbility` + `RendAbility` + tests. Fix
  `CooldownsTest` lane references.

## Acceptance

- Pure JUnit: `bucketForCurrent` 20/20→10, 18/20→9, 10/20→5, 0/20→0;
  `empowerFor` 0→0, 1→7, 10→16 (no target term), decision-owned ids pinned
  (`speed`, `entity.wind_charge.wind_burst`, `block.anvil.land`); Rush fires at
  0 stacks full HP, blocks only on cooldown/wrong-class; Rend blocks 0
  stacks, fires 1+; fire arms only its own lane; lanes advance
  independently, reset readies both and clears both windows; Rush bonus and
  Rend empower each consumed once.
- `gradle test` green. No wiring/applier changes in this ticket.

## Comments

None yet.
