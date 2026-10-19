# Spec: Copy pass — Burden and Momentum card copy

Status: done

## Card check seen

The Juggernaut card was reopened in game on the deployed jar (2026-10-19): the split rows
and the shorter Burden line read well, and the instance log held no mod exception. Follow-up
from the check: the tier rows read as standalone general passives rather than one bar's
checkpoints — folded into the card colour pass (grilling 2026-10-19).

## Goal

A short player-copy round settled by a grilling session (Oct 19 2026): Burden's card line
drops its flavour sentence and the weight metaphor leaves the reader-facing prose; Momentum's
card line splits into a rules line plus one row per checkpoint so it scans instead of reading
as a paragraph.

## Decisions

- **Burden.** Card line reads `x1.15 damage, -0.4 attack speed.` The module javadoc keeps
  "slow and heavy" but loses the weight metaphor, and so do GLOSSARY's Burden entry,
  `PROJECT_DESCRIPTION.md`, and Momentum's and Offload's javadoc flavour. Identifiers
  (`Tier.weight`, `momentum_weight`, `SCUTUM_WEIGHT`) and the card renderer's font "weight"
  wording stay.
- **Momentum, generated.** `ConditionalPassive.fullDesc()` becomes `rulesDesc()` (earns, cap,
  bleed rate) plus `tierDesc(tier)`; `ClassPresentation.cardLinesFor` draws the rules line
  (`Momentum — Land a hit: +1. Take a hit: +1. Max 60. 5s no feed: -1/s.`) then one line per
  checkpoint named `Steady at 15`, `Braced at 30`, `Rooted at 45`. Any future bar splits the
  same way; the numbers still come from the bar's own tiers.
- **Surfaces.** The HUD chips, `/class` output and wire protocol are untouched: the change is
  card copy only.

## Testing

- `MomentumTest` pins the rules line and each tier description; `ClassRegistryTest` pins the
  generated-from-own-numbers rule with a custom bar; `ClassPresentationTest` pins the card
  order (rules line, then one row per checkpoint before Burden).

## Card check owed

Reopen the Juggernaut card in game on the deployed jar and confirm the split rows and the
shorter Burden line read well; read the instance log before trusting it.
