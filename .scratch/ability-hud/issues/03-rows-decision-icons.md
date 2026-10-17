# 03: Rows decision — icons and whole seconds

Status: done

## Context

The rows decision is the one half of the scrapped HUD chain that never failed a build (ADR-0018). It returns with two changes: chips carry an icon texture id beside their badge, and countdowns print whole seconds.

## Goal

- Rewrite `StatusHud` as the pure stack decision: `rows(status, elapsedMs)` returns the roster per class — stance row (Legionary), signature lane row, ultimate lane row, Momentum row, Streak row — with riders on their lane's row (`IMP`, `TES`, the stride/empower bare numbers, the Roulette initial, `AEQ`, `ALL`).
- `Chip(Identifier icon, String badge, String value, boolean lit)`: the stance, lane and state chips carry their icon; riders carry a badge and no icon; a ready lane is `lit=false` with an empty value.
- Icon constants live on `StatusHud`, one per texture under `infusedclasses:textures/gui/ability/`; ability-to-icon mapping reads the class definition's abilities (`hasAbility`), never a new id list.
- Whole-second formatting: `ceil` round (`(ms + 999) / 1000`, `%ds`, `Locale.ROOT`), clamped at zero; a lane with no time left is ready. Riders and rows disappear exactly when their window or state dies, same boundaries as before.
- Zero keeps drawing nothing; a classless or unknown class id renders no rows.

## Done when

`StatusHudTest` pins every roster shape, rider boundary and rounding boundary from the spec; `gradle test` is green.

## Comments

Implemented: `StatusHud.rows` with `Chip(icon, badge, value, lit)`, ten icon constants, the full roster and riders, whole-second ceil countdowns. `StatusHudTest` covers roster shapes, rider boundaries, rounding at 60_000/59_999/9_999/999/1 ms, Zero, classless and unknown; green.

Superseded in look by 06 (ADR-0021): `rows` became `stack` with `Chip(label, value, lit, rider)` — words in, icon constants and badges out, the Rush `+` dropped, the Roulette rider keeps its band word. The rounding, ready/lit and boundary decisions here carried over unchanged.
