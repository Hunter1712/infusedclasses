# Spec: Legionary Gladius/Scutum stances + universal charged bonus curve

Status: done

## Problem Statement

Melee has one playable kit (Juggernaut: dive topped, build `Momentum` on any
hit, spend via `Rend`) and no reason to swing cleanly. Spam earns the same
stacks as full swings, and with a flat bonus spam out-DPSes timing:
wood sword (4 damage, 1.6 speed) with a flat +5 bonus deals ~23.1 DPS
spamming at 0.4 charge versus ~14.4 DPS timed. There is also no
damage-versus-defence tradeoff class: nothing that hits hard but bleeds, or
tanks but tickles.

## Solution

A new melee class, **Legionary**, with two Roman stances on one body plus a
universal melee rule that fixes spam for every class.

- **Stances (aura while active):** `R` (`Rotate`, 8s) toggles `Gladius`/
  `Scutum`. No build-up, no stacks. Picking a stance applies its aura at once;
  leaving removes it at once. `Gladius`: `attack_damage` +2.0
  (`infusedclasses:gladius_damage`), `attack_speed` +0.2
  (`infusedclasses:gladius_alacrity`), melee taken x1.2. `Scutum`: `armor` +6
  (`infusedclasses:scutum_plate`), `armor_toughness` +1
  (`infusedclasses:scutum_toughness`), melee dealt x0.8, `movement_speed` -8%
  (`infusedclasses:scutum_weight`). Negative-armor modifiers are avoided
  entirely (armor reads back 0 under negatives); the downsides ride in the
  damage hooks as multipliers, never as negative attributes.
- **`G` (50s shared lane) — tradeoff windows, 6s each, one key, stance decides:**
  `Gladius Impetus`: +3 hook bonus damage on every melee hit inside the window
  (through the curve, so spam earns chips), melee taken x1.25 (total taken
  x1.5 with stance). Kills 20 HP in 2 full hits with a typical sidearm, dies
  in 2 hits.
  `Scutum Testudo`: +8 armor (`infusedclasses:testudo_plate`) + no knockback,
  melee dealt x0.7 (total dealt x0.56 with stance). Lives ~9 swings, needs ~7
  to kill. Firing one locks the other; respawn, logout and class switch clear
  windows and lanes and reset stance to `Scutum`.
- **Universal melee rule:** stacks need a full swing (`attackStrengthScale ==
  1.0`); spam earns zero stacks. Damage needs no gate: mod bonus damage
  scales quadratically (`bonus x charge^2`, half charge = quarter bonus).
  Base vanilla damage is untouched — vanilla already halves spam, and the mod
  adds no second cut to it. Spam still chips vanilla; full swings get the full
  bonus. With quad, the wood-sword case flips to ~8.4 DPS spam versus 14.4 DPS
  timed.
- **No custom HUD.** No stance row, no window row, no sync signal. Feedback is
  chat lines via the shared announcer, cast sounds on FIRED only, and the
  vanilla effect icon where one exists. Windows are timestamps on the transient
  runtime, so milk-proof; death ends them.

Loop: enter in `Scutum`, `Rotate` to `Gladius` to punish, `Impetus` to close
or `Testudo` to outlive. Cycling pays; camping pays nothing.

## User Stories

1. As a Legionary, I want `R` to flip stance any time off its 8s lock, so that
   form is a decision, not a build-up.
2. As a `Gladius` Legionary, I want +2 damage and faster swings at the cost of
   taking x1.2, so that glass reads as glass.
3. As a `Scutum` Legionary, I want +6 armor and toughness at the cost of
   dealing x0.8 and moving slower, so that plate reads as plate.
4. As a `Gladius` Legionary, I want `G` to open a 6s `Impetus` (+3, take
   x1.25), so that I trade life for TTK.
5. As a `Scutum` Legionary, I want `G` to open a 6s `Testudo` (+8 armor, no
   knockback, deal x0.7), so that I trade TTK for life.
6. As a spammer on any melee class, I want zero stacks and quarter bonus at
   half charge, so that mashing earns chips, not kills.
7. As a timed swinger, I want full stacks and full bonus on full swings, so
   that rhythm wins (14.4 vs 8.4 DPS on wood).
8. As a picker browser, I want copy generated from the ability constants, so
   that text cannot drift from behaviour.

## Implementation Decisions

- `ClassRegistry`: new `LEGIONARY` (`infusedclasses:legionary`, display
  `Legionary`, playable), accent bronze `0xFFB87333` (triage if it clashes),
  tagline `Sword and shield`; actives `[rotate, impetus, testudo]` with copy
  built from constants; ids `infusedclasses:rotate`, `infusedclasses:impetus`,
  `infusedclasses:testudo` (`G` morphs by stance on a shared lane; the card
  names all three). New attribute keys `MOVEMENT_SPEED`, `ARMOR_TOUGHNESS` and
  `KNOCKBACK_RESISTANCE` (+1 brace while Testudo is live)
  next to the existing `ARMOR`/`ATTACK_DAMAGE`/`ATTACK_SPEED` keys. No
  migration (new id); Ranger/Mage untouched.
- New `RotateAbility`: `NAME` "Rotate", `ROTATE_MS` 8_000, kinds
  `FIRED`/`WRONG_CLASS`/`ON_COOLDOWN`; `trigger` pure read-only (class gate
  -> cooldown gate, never fizzles on stance); `fire` gates and flips the
  runtime stance together. FIRED announces the new stance (`Gladius!` red /
  `Scutum!` gray), no cast word. Rejects arm nothing.
- New `ImpetusAbility` + `TestudoAbility` (`G`, form-morph, shared lane):
  `IMPETUS_BONUS` 3.0F, `IMPETUS_TAKEN_MULT` 1.25 (`Ha!`), `TESTUDO_ARMOR` 8.0,
  `TESTUDO_DEAL_MULT` 0.7 (`Hold!`), each `WINDOW_TICKS` 120 (6s), shared
  `OATH_MS` 50_000 lane in `Cooldowns` (`useOath`/`oathRemaining`, read by
  both); `trigger`/`fire` gate-and-arm shape; `bonusFor()` and
  `multFor(incoming)` pure. Firing one arms the shared lane, so one fire locks
  both for 50s. Chat `Impetus!` red / `Testudo!` gray.
- Stance auras: `StanceApplier` applies only the current stance's modifiers
  (Gladius damage/speed, Scutum armor/toughness/weight) and removes the other
  stance's, same write-only-on-change shape as the Vigor tick. Stance mults
  (Gladius take x1.2, Scutum deal x0.8) and window mults (Impetus take x1.25,
  Testudo deal x0.7) are decided in `MeleePlan` and settled in `DamageSettle`;
  no negative attribute modifiers anywhere.
- New pure `BonusCurve`: `bonusFor(base, charge) = base x charge x charge`
  (1.0 full, 0.25 half, ~0 spam), applied to mod hook bonus damage only
  (`Rend` empower, `Impetus` +3, future payoffs). Base weapon damage is never
  touched. One call site in the attacker branch; every melee class shares it,
  no per-class copies.
- Stack gate: attacker branch reads `attackStrengthScale` once (exact signature
  confirmed via `javap` against the 1.21.11 merged jar at implementation time,
  same as the `warrior-procs` note) and feeds `PlayerRuntime.onTrigger` only at
  full charge (`MeleePlan.shouldBump`). `Juggernaut Momentum` grace 4s -> 5s to
  cover the swing timer under the new gate. Unknown triggers stay inert.
- `PlayerRuntime`/`PlayerRuntimes`: holds `stance` (default `SCUTUM`), `rotate`
  lane, shared `oath` lane, `Impetus`/`Testudo` window timestamps;
  respawn/switch/logout clears windows, resets lanes, resets stance to
  `SCUTUM`. Choice itself stays in `PlayerClasses`; stance is transient per
  ADR-0002.
- Payloads/keys: `R` keeps its bind as the `rotate` intent, `G` keeps its bind
  as the `oath` intent (same bind-survival rule as the Rush/Rend rename).
- Copy/docs from constants: `ActiveAbility` descs (Rotate lock + stance
  announces, Impetus bonus/taken/window, Testudo armor/deal/window, shared 50s
  lane), plus `GLOSSARY.md` (Legionary, Gladius, Scutum, Rotate, Impetus,
  Testudo, BonusCurve, stack gate) and one ADR for the stance + quad decisions.

## Testing Decisions

- Pure JUnit on decisions: `bonusFor` at charges 0.0/0.5/1.0 (0/0.25x/1.0x);
  `dealMult`/`takeMult` matrix (Gladius/Gladius+Impetus = x1.2/x1.5 taken,
  Scutum/Scutum+Testudo = x0.8/x0.56 dealt); Rotate fires any stance / rejects
  arm nothing; shared G lane (fire one locks other 50s); stance reset to
  `SCUTUM` on clear.
- Runtime: spam feeds zero stacks; full feeds one; grace re-stamp; windows
  live/expiry; milk changes nothing (timestamps, not effects).
- Registry/presentation: Legionary carries its ability ids, copy reflects live
  numbers, no `Roots` text, no HUD rows.
- Not tested: sound audibility, Speed/knockback feel, pack balance — manual
  dev-client verification (`gradle deployClient`, `/class select <player>`).

## Out of Scope

- Touching Juggernaut numbers beyond the Momentum grace 4s -> 5s,
  Ranger/Mage, respawn/logout semantics beyond window+stance clear, any HUD
  rows or sync signals.
- Ranged/spell credit (melee only, same direct-entity check as the Momentum
  hook), velocity/dash motion (effects only), lifesteal/absorption-heal
  economies, block/shield triggers.
- Balance tuning beyond these numbers — next round after playtest.
