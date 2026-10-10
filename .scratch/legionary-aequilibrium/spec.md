# Spec: Legionary switch buffs (Impetus/Testudo) + Aequilibrium ultimate

Status: done

## Problem Statement

Legionary identity sits in always-on stance auras (`Gladius` +2.0 damage / take
x1.2, `Scutum` +6 armor / deal x0.8) plus the stance-morphed oath pair on `G`
(`Impetus` +3 through the curve / take x1.25, `Testudo` +8 armor and brace /
deal x0.7, 6s windows on a shared 50s lane). Camping a stance pays full value
with no further decision after the `Rotate`. Agreed direction: halve the
always-on stats, pay for the flip itself under the existing names, and give
`G` one clean ultimate.

## Solution

- **Base stances stay, nerfed.** `Gladius`: damage +2.0 -> +1.0, taken x1.2 ->
  x1.1, speed +0.2 -> +0.3 (pays back part of the damage cut as swing rate).
  `Scutum`: armor +6.0 -> +3.0, dealt x0.8 -> x0.9, weight -0.008 -> -0.006
  (about -6% of the 0.1 base). Toughness +1.0 unchanged. Glass still reads as
  glass, plate as plate, at half tax.
- **Switch buffs reuse the old names (no new vocabulary).**
  `Impetus` becomes the Gladius-flip buff: next-hit deal x1.10, any charge
  (explicit deviation from the full-swing stack gate), consumed on the hit,
  expires 5s after the flip so the buff cannot be held. `Testudo` becomes the
  Scutum-flip buff: take x0.85 for 1.5s from the flip. Both encourage the
  flip itself; sitting between flips earns nothing extra.
- **Old oath windows gone, names moved.** `Impetus` no longer means +3 over
  6s on the oath lane; `Testudo` no longer means +8 and brace over 6s. The
  6s-window + shared-lane machinery moves to the new ultimate. The 8s
  `Rotate` lock is now the only gate on both switch buffs.
- **New G ultimate: Aequilibrium.** `infusedclasses:aequilibrium`, display
  `Aequilibrium`, on `G` in either stance (no morph). 6s window
  (`WINDOW_TICKS` 120), shared 50s lane (reuse `Cooldowns.OATH_MS`,
  `useOath`/`oathRemaining` verbs unchanged, now owned solely by
  Aequilibrium). Effect: +1.0 attack damage
  (`infusedclasses:aequilibrium_edge`) + +3.0 armor
  (`infusedclasses:aequilibrium_plate`), no deal/take mults, no knockback
  immunity, no downsides. Overlay: stance stays underneath, both layers count
  (switch x1.10 multiplies the +1; x0.85 multiplies under the +3). Peak combo
  is gated by the 8s `Rotate` lock plus the 50s lane.
- **Seam.** `PlayerRuntime.flipStance()` stamps nothing today, so add one flip
  record (`flipAt` millis + `flippedTo` stance) next to the old oath windows
  (which become the single Aequilibrium window). The Impetus buff peeks in
  `MeleePlan.planHit` and spends in the hit settle like `consumeRushBonus`;
  the Testudo buff is peeked by `takeFor` for 1500ms. `flipStance` gains a
  time/landing-stance signature; its three callers (`RotateAbility.fire`,
  tests) move together.
- **Copy from constants.** `Rotate` desc gains the switch lines, the
  `Impetus`/`Testudo` descs are rewritten from the new numbers (flip buffs,
  not oath windows), the `Aequilibrium` desc is built from its constants.
  Card actives become `[rotate, impetus, testudo, aequilibrium]`. No HUD, no
  sync signal: chat lines plus cast sound on FIRED only, timestamps on the
  transient runtime, death ends windows.

Loop: enter `Scutum`, `Rotate` to `Gladius` for the Impetus x1.10 hit, `G`
for the clean Aequilibrium 6s when the fight starts, `Rotate` back for the
Testudo x0.85 catch.

## User Stories

1. As a Legionary, I want the flip to pay even before ultimate timing, so
   that form is a decision every 8s.
2. As a `Gladius` Legionary, I want +1 damage at x1.1 taken, so that glass
   still bleeds.
3. As a `Scutum` Legionary, I want +3 armor at x0.9 dealt, so that plate still
   tickles.
4. As a Legionary, I want `G` to open a clean 6s (+1 damage, +3 armor, no
   downside) in either stance, so that the ultimate never punishes casting it.
5. As a picker browser, I want copy generated from the ability constants, so
   that text cannot drift from behaviour.

## Implementation Decisions

- `RotateAbility`: retune `GLADIUS_DAMAGE` 2.0 -> 1.0, `GLADIUS_TAKEN_MULT`
  1.2 -> 1.1, `GLADIUS_SPEED` 0.2 -> 0.3, `SCUTUM_ARMOR` 6.0 -> 3.0,
  `SCUTUM_DEAL_MULT` 0.8 -> 0.9, `SCUTUM_WEIGHT` -0.008 -> -0.006;
  `SCUTUM_TOUGHNESS` 1.0 unchanged. `auraFor`/`dealMult`/`takeMult` keep
  shape, new numbers only.
- `ImpetusAbility`: repurposed to the flip buff. `IMPETUS_BONUS` deleted;
  new `SWITCH_DEAL_MULT` 1.10, `SWITCH_DEAL_MS` 5_000. Trigger reads the flip
  record (flipped to `Gladius` inside 5s), not the oath lane. One-shot:
  peek/consume verbs mirror Rush. No trigger/fire remains: nothing fires Impetus
  once `G` means Aequilibrium, so the oath announcement went with the oath path.
- `TestudoAbility`: repurposed to the flip buff. `TESTUDO_ARMOR` 8.0,
  `TESTUDO_DEAL_MULT` 0.7, plate/brace aura deleted; new `SWITCH_TAKE_MULT`
  0.85, `SWITCH_TAKE_MS` 1_500. The buff peeks the flip record (flipped to
  `Scutum` inside 1.5s), not the oath lane. No trigger/fire remains, same as Impetus.
- `PlayerRuntime`: one flip record (`flipAt`, `flippedTo`); `flipStance(now,
  landing)` stamps both; the two oath windows collapse into the single
  Aequilibrium window; `resetCooldowns` clears flip + window and resets
  stance to `SCUTUM`.
- `MeleePlan`: `dealFor` multiplies x1.10 while the Impetus flip buff is
  pending (any charge per agreement, curve still applies to hook bonuses);
  `takeFor` multiplies x0.85 while the Testudo flip window is live. Old
  oath-window branches deleted. Combined peak (switch + Aequilibrium) is
  intentional; gates hold it.
- New `AequilibriumAbility`: `NAME` "Aequilibrium", `EDGE_BONUS` 1.0,
  `PLATE_BONUS` 3.0, `WINDOW_TICKS` 120, cast `Aequilibrium!` gold;
  `trigger`/`fire` in the gate-and-arm shape on the reused oath lane, fires
  in either stance, wrong-class only for non-Legionaries. Rejects arm
  nothing. Window modifiers ride `StanceApplier` while live,
  write-only-on-change, single aura list for write/remove.
- `ClassRegistry`: `LEGIONARY` actives become `[ROTATE_ABILITY,
  IMPETUS_ABILITY, TESTUDO_ABILITY, AEQUILIBRIUM_ABILITY]`; new id
  `infusedclasses:aequilibrium` plus `infusedclasses:aequilibrium_edge` /
  `infusedclasses:aequilibrium_plate`. Impetus/Testudo ids stay, descs move
  to flip buffs.
- `BindMorph.OathTarget`: drop `IMPETUS`/`TESTUDO`, add `AEQUILIBRIUM`;
  `oathFor` returns it for any Legionary stance (morph deleted). `G` never
  meant Impetus/Testudo again. `InfusedClasses` oath dispatch follows the
  enum rename.
- `StanceApplier`: old Testudo plate/brace branch replaced by the
  Aequilibrium window aura; write and remove iterate the single aura list so
  they cannot drift. Leaving the class takes stance + ultimate + flip state
  off.
- Copy/docs from constants: `Rotate` desc (switch lines), rewritten
  `Impetus`/`Testudo` descs (flip buffs), new `Aequilibrium` desc
  (bonus/window/lane), plus `GLOSSARY.md` (Gladius, Scutum, Rotate,
  Impetus/Testudo as flip buffs, new Aequilibrium entry) and one ADR for
  the stance-to-switch plus ultimate-swap decisions.

## Testing Decisions

- Pure JUnit: retuned `dealMult`/`takeMult` matrix (1.1 / 0.9); Impetus flip
  buff fires on flip-to-Gladius, consumed once, expires at 5s, any charge;
  Testudo flip window live/expiry at 1.5s, Scutum-gated; Aequilibrium fires
  either stance, arms shared lane 50s, window live/expiry 6s, overlay keeps
  stance mults; flip record cleared on reset; combined peak equals base x
  switch + ultimate (no override).
- Registry/presentation: Legionary carries `[rotate, impetus, testudo,
  aequilibrium]`, copy reflects live numbers, no oath-window text, no HUD
  rows.
- `gradle test` green. Manual: `gradle deployClient`, `/class select`,
  verify 2-hit TTK feel and the 1.5s catch window in game.
- Not tested: sound audibility, Speed/knockback feel, pack balance beyond
  these numbers; next round after playtest.

## Out of Scope

- Touching Juggernaut numbers, Ranger/Mage, respawn/logout semantics beyond
  flip+window clear, any HUD rows or sync signals.
- Ranged/spell credit (melee only), velocity/dash motion (effects only),
  lifesteal/absorption economies, block/shield triggers.
- Balance tuning beyond these numbers; next round after playtest.
