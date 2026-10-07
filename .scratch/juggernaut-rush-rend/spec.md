# Spec: Juggernaut Rush + Rend (Berserker full rename)

Status: done

## Problem Statement

Berserker's kit is dead for players who kite. Every piece demands getting
hurt while topped-off:

1. `Bulwark` needs `on_hit_taken` (`ServerEvents.java:45`), `Fury` needs
   missing HP (`HpScaledPassive.java:34-43`), `R Bloodlust` fizzles above
   ~5 hearts (`BloodlustAbility.java:19-20`), `G Second Wind` fizzles at
   0 stacks. A cautious player gets zero passives and two fizzles.
2. Bloodlust heals live HP, directly cooling the Fury bucket that powered
   it; `R` also grants free `bucket` Bulwark, enabling an `R -> G`
   autopilot with no tanking required.
3. Both actives root 1s via `Spellcast.java:13-14` (`SLOW_TICKS`,
   `SLOW_AMPLIFIER`), applied in `AbilityApplier.java:50-55` and
   `BloodlustApplier.java:31`. Rooting a melee engage and a chase
   execute self-cancels.

## Solution

Full rename plus a high-HP inversion. Berserker becomes **Juggernaut** —
scary full, vulnerable empty. Dive topped, build by hitting, spend before
you bleed out. Two sounds replace the shared root: a wind-burst whoosh for the engage,
an anvil-land smash for the execute.

- **Rename**: `Berserker` → `Juggernaut`, full rename (id
  `infusedclasses:juggernaut`, display `Juggernaut`). Passive group `Fury`
  → `Vigor` (`Vigor Damage` / `Vigor Speed`), numbers unchanged. Old
  `infusedclasses:berserker` picks map forward to Juggernaut on load, no
  wipes. Colour/tagline unchanged unless triage says otherwise.
- **Vigor (replaces Fury):** same per-steps as Fury (`0.3` damage /
  `0.05` speed, `STEPS` 10) but buckets on **current** HP, not missing.
  Full = +3.0 / +0.5, 0 HP = nothing. Recomputed every tick like
  `PassiveApplier.java:39-47`; heals now help instead of cooling.
- **Momentum (replaces Bulwark):** `on_hit_dealt`, max 10, `minecraft:armor`
  under `infusedclasses:momentum_armor`, +1.0 per stack, 4s grace,
  lose-all. Tank earned by landing hits, not taking them.
- **Rush (R, 25s):** 4s Speed I + **cleanse every active HARMFUL effect**
  (poison, wither, weakness, slowness, blindness — adapter drops harmful
  only, beneficial kept) + next melee hit inside the window grants +3
  Momentum (cap-respecting, grace re-stamped). Always fires off cooldown;
  never fizzles on HP/stacks. Sound `minecraft:entity.wind_charge.wind_burst` with copy
  tail `RushAbility.CAST_WORD` (`Whoosh!`).
- **Rend (G, 60s):** consumes all Momentum up front and empowers the next melee
  hit landed within 6s for `6 HP + 1 HP/stack` bonus damage (6–16 HP). No target-HP
  check, no press-time target: `G` fires with stacks and the bonus rides the next
  swing. An unswung window expires with the stacks already spent. Fizzles
  free at 0 stacks (`No Momentum`-style line). Sound
  `minecraft:block.anvil.land` with copy tail `RendAbility.CAST_WORD` (`Smash!`).
- **Root removed globally:** `SLOW_TICKS` / `SLOW_AMPLIFIER` and
  `applyCastSlow` go away; every cast plays its own sound on FIRED only
  instead. Fizzles (wrong-class, cooldown, no-momentum) stay silent and
  touch nothing. Picker copy drops all `Roots you for 1s` clauses. Fizzles play no
  sound; the cooldown line is the only chat they send (wrong-class stays fully silent).

Loop: dive topped (Vigor hot) -> `R` in (cleanse + stick) -> land hit
(+3) -> build to 6–10 -> `G` execute. Failed dive still punishes: low HP
means no Vigor and no sustain — that is the intended risk.

## User Stories

1. As a returning Berserker pick, I want to load as Juggernaut, so that
   the rename strands nobody.
2. As a topped-off Juggernaut, I want full damage/speed without taking a
   hit first, so cautious play still functions.
3. As an engaged Juggernaut, I want landing hits to build armor, so
   staying on target is the tank loop.
4. As a kited Juggernaut, I want `R` to cleanse slows/poison and stick
   with Speed, so archers cannot peel me for free.
5. As a chasing Juggernaut, I want `G` to empower my next swing for
   `6 + stacks`, so the finisher lands when I choose to swing, not at press time.
6. As a fat-fingered Juggernaut, I want `G` at 0 stacks to fizzle free
   (cooldown untouched, silent except the line), like Second Wind today.
7. As a picker browser, I want copy generated from the new constants
   (Juggernaut, Vigor steps, Momentum cap/grace, Rush window/cooldown/
   cleanse, Rend base/per-stack/execute/cooldowns, cast words (`CAST_WORD`)), so text
   cannot drift from behaviour.

## Implementation Decisions

- `ClassRegistry`: `BERSERKER` → `JUGGERNAUT`, id
  `infusedclasses:berserker` → `infusedclasses:juggernaut`, display
  `Berserker` → `Juggernaut`, colour/tagline unchanged;
  `Fury Damage` / `Fury Speed` (group `Fury`) -> `Vigor Damage` /
  `Vigor Speed` (group `Vigor`), per-steps unchanged; `Bulwark`
  (`on_hit_taken`, 20 x 0.5, grace 5) -> `Momentum` (`on_hit_dealt`,
  10 x 1.0 armor, grace 4); actives `[bloodlust, second_wind]` ->
  `[rush, rend]` with copy built from constants; ids
  `infusedclasses:rush`, `infusedclasses:rend`.
- `HpScaledPassive`: add `bucketForCurrent(currentHp, maxHp)` (mirror of
  `bucket`, floor(current * STEPS), clamp 0–10); `PassiveApplier` Vigor
  tick uses it, `fullDesc` notes current-HP wording. Keep `bucket`
  (missing-HP) for tests/back-compat or delete with callers — one or
  the other, no dead helper.
- `ConditionalPassive`: add `ON_HIT_DEALT = "on_hit_dealt"` next to
  `ON_HIT_TAKEN`; `PlayerRuntime.onTrigger` already generic, no change.
- New `RushAbility`: `NAME` "Rush", `SPEED_TICKS` 80 (4s), speed
  amplifier 0, `BONUS_STACKS` 3, `RUSH_MS` 25_000, kinds
  FIRED/WRONG_CLASS/ON_COOLDOWN; `trigger` pure read-only (class gate ->
  cooldown gate, no HP/stack gate), `fire` gates and arms together.
  Window = press timestamp on runtime, live 4s; next-hit bonus consumed
  on first qualifying melee hit.
- New `RendAbility`: `NAME` "Rend", `BASE_HP` 6.0F, `HP_PER_STACK` 1.0F,
  `WINDOW_TICKS` 120 (6s), `REND_MS` 60_000, kinds
  FIRED/WRONG_CLASS/ON_COOLDOWN/NO_MOMENTUM; `empowerFor(stacks)`
  pure (`stacks <= 0 -> 0`, else `6 + stacks`); `momentumOf(def)` trigger-found like
  `SecondWindAbility.bulwarkOf`, never by position; `trigger`/`fire`
  gate-and-arm shape; consume flag rides in result, empower snapshot rides the
  transient runtime next to the lane.
- `Cooldowns`: `BLOODLUST_MS`/`SECOND_WIND_MS` -> `RUSH_MS` 25_000 /
  `REND_MS` 60_000 with `useRush`/`rushRemaining`,
  `useRend`/`rendRemaining`; `reset` readies both. `PlayerRuntime`
  exposes lanes plus Rush window (`rushPressedAt`, `isRushLive`,
  `consumeRushBonus` one-shot flag); respawn/switch clears windows with
  everything else.
- `ServerEvents.AFTER_DAMAGE`: victim branch feeds `ON_HIT_TAKEN` only
  if a class still uses it (else removed with Bulwark); attacker branch
  (existing melee check at `ServerEvents.java:51-52`) feeds
  `ON_HIT_DEALT` for Momentum (+1, grace re-stamp), grants +3 when a Rush
  window is live with bonus unconsumed, then spends a live Rend empower
  snapshot as bonus damage on the same hit. The extra hurt re-enters the hook
  on the same thread, guarded by a flag so it earns nothing twice.
- Adapters: `RushApplier` (Speed + cleanse + announce + wind burst on FIRED),
  `RendApplier` (damage + consume via `PassiveApplier.consumeSpent`-shaped
  path + announce + anvil land on FIRED). Shared `announce` stays in
  `AbilityApplier`. Cleanse iterates `player.getActiveEffects()` and
  calls `removeEffect` for harmful-category holders only; category
  lookup resolved via `javap` against the merged jar (1.21 effect
  signatures drifted — no guessing, resolve `MobEffect#getCategory` /
  `MobEffectCategory` first). Sound via `level.playSound` with
  `SoundEvents` holders (wind-burst and anvil-land holders resolved via `javap`
  against the merged jar like the category lookup; fail silent if missing).
- `Spellcast`: `SLOWNESS`/`SLOW_TICKS`/`SLOW_AMPLIFIER` deleted with
  `applyCastSlow` and all callers; module becomes the cast-sound rule
  (per-ability sound id constants or one helper `playCastSound(level,
  pos, holder)` — implementer's call, but one place, not per applier).
  Delete `BloodlustAbility`/`SecondWindAbility`/`BloodlustApplier` heal
  paths only when no caller remains; `grep` before removing.
- Payloads/keys: `R` reuses bloodlust key verb (`rush` intent), `G`
  reuses second-wind verb (`rend` intent) — rename payloads, keep keys
  `R`/`G` so binds survive. Rend needs no press-time target; the empower
  waits for the next swing.
- Migration: `PlayerClasses` load maps stored `infusedclasses:berserker`
  to `infusedclasses:juggernaut`.
- As-built (fe24f6d, seam detail in ADR-0005): stack storage moved to
  `PassiveStacks` behind the runtime seam; `ClassDefinition.passiveForTrigger`
  is the single trigger scan with `RendAbility.momentumOf` kept as its alias;
  the damage hook runs for any attacker with a class (no ability-id
  comparison); lane liveness shares one `isLive` helper and cooldown reads
  share one `remaining` helper; `SPEED_TICKS` is tied to the Rush window;
  `AbilityApplier` exposes the shared holder/`playCast` seams.
- Copy/docs from constants: `ActiveAbility` descs (Rush window/cooldown/
  cleanse/sound, Rend base/per-stack/execute/cooldown/sound, no root
  clause), `ConditionalPassive.fullDesc` (Momentum), Vigor desc
  (current-HP wording), plus `GLOSSARY.md` (Berserker→Juggernaut,
  Fury→Vigor, Bulwark→Momentum, Bloodlust/Second Wind→Rush/Rend,
  Spellcast root→sound, cleanse) and an ADR for the inversion + root
  removal.

## Testing Decisions

- Pure JUnit on decisions: `bucketForCurrent` at 20/20, 18/20, 10/20,
  0/20 (10/9/5/0 buckets); `empowerFor` at stacks 0/1/10 (0, 7, 16, no
  target-HP term); Rush fires with 0 stacks at full HP / fizzles
  only on cooldown or wrong class; Rend blocks 0 stacks / fires 1+; fire
  arms only its own lane; Rush bonus consumed once, rejects arm nothing.
- Runtime: `ON_HIT_DEALT` bumps Momentum capped at 10 with grace
  re-stamp; unknown triggers inert; Rush window live/expiry; respawn
  and switch clear windows + reset both lanes; Rend consume drops only
  the priced passive.
- Registry/presentation: Juggernaut carries `[rush, rend]` in order, old
  Berserker id resolves forward, copy reflects new numbers (empower window,
  no execute), no `Roots` text remains.
- Not tested: sound audibility, Speed feel, cleanse coverage across
  every effect, pack balance — manual dev-client verification
  (`gradle deployClient`, `/class select <player>`).

## Out of Scope

- Touching Ranger/Mage, respawn/logout semantics beyond window clear,
  HUD rows.
- Ranged/spell Momentum (melee only, same direct-entity check as the
  old lifesteal hook), window caps beyond durations, charge/leap motion
  (Rush is effects-only, no velocity).
- Balance tuning beyond these numbers — next round after playtest.
