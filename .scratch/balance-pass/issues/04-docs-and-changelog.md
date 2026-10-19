# 04 — Docs and changelog

Status: done
Spec: `.scratch/balance-pass/spec.md`

## What

Move every doc with the behavior.

- `GLOSSARY.md`: "Full swing" anchor to `FullSwing` with the curve text deleted; "Burden" (x1.15, no damage modifier id); "Conditional passive" weights flat; "Rotate"/"Gladius"/"Scutum" (x1.15, -0.005); "Aequilibrium" (x1.15 window); "Onslaught" (any-charge mult, full-swing price); "Melee plan" (combined composition, no flat bonus); "Damage settle" (no `dealBonus`); "Always-on passive" (dealMult); the ADR-history lines that mention the curve (723, 794 region).
- `docs/guide/classes.md`: Burden, Momentum weights, Offload, Gladius, Scutum, Aequilibrium, Onslaught, and delete the "charged bonus" paragraph from Shared melee rules.
- `docs/guide/stances-passives.md`: tier rows, Burden line, stance table, Impetus note if it names units.
- `docs/adr/0028-one-unit-per-stat-family.md` + `docs/adr/README.md` index line; mark 0006 point 1 and 0027 point 6 revised.
- `CHANGELOG.md` under Unreleased: one Changed entry for the unit rule + conversions, one for the movement flats, one Removed for the curve.

## Done when

`tools/doc-flags.sh` clean; GLOSSARY and the guide contain no `-2% speed`-style movement unit and no curve language.
