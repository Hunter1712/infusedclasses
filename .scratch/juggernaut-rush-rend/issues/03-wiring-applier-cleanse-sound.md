# 03: Rush/Rend intents, receivers, appliers, Momentum hook, cleanse, horn/roar, root removal

Spec: `.scratch/juggernaut-rush-rend/spec.md`

**Blocked by:** 02 (needs the decisions + lanes).

**Status:** done

## Task

Client-to-server path and the adapters that land Rush/Rend, replacing
lifesteal + shield with engage + empower:

- `RushPayload` on `infusedclasses:rush` replacing the `bloodlust` line
  (`InfusedClasses.java:26`); `RendPayload` on `infusedclasses:rend`
  replacing `second_wind`. `R`/`G` keybinds
  (`InfusedClassesClient.java`) send them; binds survive, payloads rename.
  Delete `BloodlustPayload.java` / `SecondWindPayload.java` when no caller
  remains (`grep` first).
- Receivers in `InfusedClasses.registerReceivers` replacing the Bloodlust /
  Second Wind blocks: Rush fires the 02 decision, applies, stamps the 4s
  window; Rend fires, applies (announce + roar), then consumes Momentum via the
  `PassiveApplier.consumeSpent`-shaped path (trigger-found, priced passive
  only). No press-time target: the empower waits for the next swing.
- New `RushApplier.apply(player, result)`: announce via shared
  `AbilityApplier` path, Speed I 80 ticks + cleanse + horn on FIRED only,
  ids from `RushAbility` (`SPEED_EFFECT`, `CAST_SOUND`), holders resolved via
  the shared `AbilityApplier` lookups. New `RendApplier`: announce + roar
  (id from `RendAbility.CAST_SOUND`) on FIRED only, plus `dealEmpower`
  for the hook-spent bonus damage with a re-entrancy guard. Fizzles say
  their line and touch nothing.
- Cleanse: iterate `player.getActiveEffects()`, `removeEffect` for
  harmful-category holders only (beneficial kept). Resolve category lookup
  via `javap` against the merged jar first (1.21 effect signatures
  drifted — no guessing). Resolve the raid-horn `SoundEvents` holder the
  same way as the roar; fail silent if missing.
- `ServerEvents.AFTER_DAMAGE` (`ServerEvents.java:38-52`): victim
  `ON_HIT_TAKEN` branch removed with Bulwark (unless another class uses
  it); attacker melee branch feeds `ON_HIT_DEALT` (+1 Momentum, grace
  re-stamp, cap 10), grants +3 when a Rush window is live with bonus
  unconsumed, then spends a live Rend empower snapshot as bonus damage
  through `RendApplier.dealEmpower` (guarded against re-entrancy).
- `Spellcast`: delete `SLOWNESS`/`SLOW_TICKS`/`SLOW_AMPLIFIER` with
  `applyCastSlow` and all callers; module becomes the cast-sound rule
  (per-ability sound ids or one `playCastSound` helper — one place, not
  per applier). Delete `BloodlustApplier.java` heal path and
  `WarCry`-era leftovers only when caller-free.

## Acceptance

- Rejected presses leave lanes, windows, and stacks untouched (no arming,
  no sound, no cleanse).
- Rush window expires after 4s; bonus grants once then consumed; Momentum
  caps at 10 with grace re-stamp.
- `gradle test` green. Feel (sound/speed/cleanse) is manual in 04.

## Comments

None yet.
