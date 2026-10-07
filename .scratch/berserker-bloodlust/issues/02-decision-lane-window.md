# 02: Bloodlust decision, lane, window, and scaled builder (pure)

Spec: `.scratch/berserker-bloodlust/spec.md`

**Blocked by:** 01 (needs the Berserker ids).

**Status:** wontfix

## Task

Pure decision module replacing `WarCryAbility`, mirroring the Second Wind
gate-and-arm shape plus the HP gate:

- New `BloodlustAbility`: `NAME` "Bloodlust", `WINDOW_TICKS` 200 (10s),
  `LIFESTEAL_PER_BUCKET` 0.02, `MAX_PER_HIT_HP` 2.0, `GATE_BUCKET` 5; kinds
  FIRED/WRONG_CLASS/ON_COOLDOWN/TOO_HEALTHY; `lifestealFor(bucket,
  damageTaken) = min(2.0, damage * bucket * 0.02)`; `trigger(def, cooldowns,
  bucket, now)` gates class → `bucket >= 5` → cooldown; `fire` gates and arms
  together; `Bloodlust!` red announcement, `Too healthy`-style line for the
  gate fizzle (null only for wrong class).
- `Cooldowns.java:8`: `WAR_CRY_MS` 20s → `BLOODLUST_MS` 30_000L with
  `useBloodlust`/`bloodlustRemaining`; `reset` readies both. `PlayerRuntime`
  exposes the lane verbs plus the window (`bloodlustPressedAt`/`isBloodlustLive`
  shape is implementer's call; respawn/switch clears it with everything else).
- Grant path: `PassiveApplier.grantWarCryStacks` → bucket-scaled grant
  (loop `bucket` times through `onTrigger`, capped by `maxStacks` 20, grace
  re-stamped); rejects grant nothing. `on_hit_taken` 1-per-hit untouched.
- Delete `WarCryAbility.java` + `WarCryAbilityTest.java`, replaced by
  `BloodlustAbility` + `BloodlustAbilityTest`. Fix `CooldownsTest`,
  `ClassPresentationTest` lane references.

## Acceptance

- Pure JUnit: lifesteal at buckets 0/5/10 × damage 7/30 (0, ~0.7, cap clip to
  2.0); gate blocks bucket 4 / fires bucket 5; builder 0/5/10 at buckets
  0/5/10; fizzle/too-healthy/wrong-class arm nothing; fire arms only its own
  lane; 30s/60s lanes advance independently, reset readies both.
- `gradle test` green. No wiring/applier changes in this ticket.

## Comments

Triage: wontfix — superseded by juggernaut-rush-rend: implemented in 2545677,
then removed in fe24f6d, which renames Berserker→Juggernaut and replaces
Bloodlust/Second Wind with Rush/Rend. See `.scratch/juggernaut-rush-rend/spec.md`.
