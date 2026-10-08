# ADR-0006: Legionary stances plus the universal charged curve and stack gate

Status: accepted

## Context

Melee has one playable kit (Juggernaut: dive topped, build `Momentum` on any
hit, spend via `Rend`) and no reason to swing cleanly. Spam earns the same
stacks as full swings, and with a flat bonus spam out-DPSes timing: a wood sword
(4 damage, 1.6 speed) with a flat +5 bonus deals ~23.1 DPS spamming at 0.4 charge
versus ~14.4 DPS timed. There is also no damage-versus-defence tradeoff class:
nothing that hits hard but bleeds, or tanks but tickles.

## Decision

1. **Quad curve, one call site.** Mod hook bonus damage scales as
   `bonus x charge x charge` (`BonusCurve`), half charge pays a quarter, spam
   pays chips. Base weapon damage is never touched — vanilla already halves
   spam. The wood-sword case flips to ~8.4 DPS spam versus 14.4 DPS timed.
2. **Full-swing stack gate.** The attacker branch reads `attackStrengthScale`
   once per swing (`getAttackStrengthScale(0.0F)`, signature confirmed via
   `javap` against the 1.21.11 merged jar) and feeds conditional triggers only
   at 1.0. `Momentum` grace reads 5s (was 4s) to cover the swing timer.
3. **Legionary, two stances on one body.** `R` (`Rotate`, 8s) toggles
   `Gladius` (+2 damage, +0.2 speed, take x1.2) / `Scutum` (+6 armor,
   +1 toughness, about -8% speed, deal x0.8). No build-up, no stacks, no HUD:
   chat lines via the shared announcer, vanilla effect icon where one exists.
   Downsides ride the damage hooks as multipliers, never as negative attributes
   (armor reads back 0 under negatives).
4. **One `G`, stance morphs it, shared 50s oath lane.** `Gladius Impetus`
   (+3 through the curve, take x1.25, 6s) trades life for TTK; `Scutum Testudo`
   (+8 armor, no knockback via +1 resistance, deal x0.7, 6s) trades TTK for
   life. Firing one locks the other. Binds survive: `R` reuses the Rush bind,
   `G` reuses the Rend bind, reinterpreted per class on the server.
5. **Timestamps, not effects.** Stance plus both windows ride the transient
   runtime (default `SCUTUM`); respawn, logout and class switch clear windows
   and lanes and reset the stance. Milk changes nothing; death ends everything.

## Consequences

- Spam chips vanilla but earns no stacks and near-no bonus on every melee
  class; rhythm wins without touching base damage numbers.
- Bonus hurts re-enter the damage hook on the same thread, so a resolving flag
  in the entry keeps them from earning stacks twice; stance discounts settle
  as a heal-back, premiums as extra hurt through the empower path.
- `Scutum` slowness rides the existing `ADD_VALUE` write path as -0.008
  (~-8% of the 0.1 base) with a `ponytail:` ceiling note, so one slow needs no
  per-operation plumbing.
