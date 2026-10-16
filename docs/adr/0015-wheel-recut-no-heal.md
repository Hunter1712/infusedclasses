# ADR-0015: Re-cut the wheel with no healing

Status: accepted

## Context

Roulette shipped (0.2.0) as seven bands, two of which healed: Vigor (20%,
instant 3 HP) and Jackpot (8%, instant 4 HP atop its three stats). The
Gambler's free 25s-lane spin therefore doubled as a sustain button — the only
heal the mod hands out — so "press a button to get HP back" had exactly one
answer and it was chance, against the class's bet-and-lose identity; a band
whose whole payoff is a heal also sits awkwardly beside stat bands the player
has to weigh. Directive: remove all healing from Roulette.

## Decision

1. **The heal path is deleted, not disabled**: the Vigor band, `VIGOR_HEAL`,
   `JACKPOT_HEAL`, the `healHp` enum component and accessor, and the applier's
   heal branch all go. No constant or dead branch survives as a re-entry point.
2. **Six bands, odds re-cut ×1.25**: Edge 25% (×1.25 damage), Ward 25% (+2
   armor), Haste 25% (+0.02 speed), Jackpot 10% (all three stats, no heal),
   Bust 10% (×0.75), Zero 5% (dead) — 100% filled, same order and rule set
   (10s window, 25s lane, free spin, Edge/Bust read by the melee plan,
   Ward/Haste through the window-aura path).
3. **The applier folds away**: `RouletteApplier` was a one-forward adapter
   whose whole body sat beside the shared announce-and-cast — its one reason
   was the instant heal. It is deleted the way ADR-0008.2 deleted
   `RendApplier`, and the receiver calls `AbilityApplier.announceAndCast`
   directly, as All In and Aequilibrium already do.
4. **Copy regenerates**: the wheel's desc prints the six new percents and no
   heal text, still generated from the band tops.

## Consequences

- The wheel is stats-only; the Gambler's sustain is vanilla-only like every
  other class, and the wheel's expected value drops by the removed heals.
- The GLOSSARY's Ability applier sentence ("only Rush and Roulette keep their
  own adapters — Rush for the cleanse and the stride it lays, Roulette for the
  instant heal") loses its Roulette half; Rush's adapter remains the one kept.
- Band-cut, odds, mult and aura tests re-pin the six bands; the heal
  assertions are deleted rather than inverted, because there is nothing left
  to assert.
- One less ability adapter in the tree — the deletion test is satisfied.
