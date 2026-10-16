# 02: Rows decision + one-decimal countdowns

Spec: `.scratch/status-hud/spec.md`

**What to build:** Given the last snapshot and the millis since it arrived, a pure decision lays out the whole strip as rows: which chips a class shows, in what order, their badges and labels, which are dim (ready) versus lit (cooling or live), countdowns as `%.1fs`, and the `+` marker on the stride rider while the engage bonus is unspent. Zero renders no chip; a classless player no rows. Chat's shared cooldown line drops to the same one-decimal precision, so chat and the strip can never disagree about how a countdown reads.

**Blocked by:** 01 — the snapshot's field set.

**Status:** done

- [x] Rows for all three classes at boundary snapshots: lane rows always present (dim ready, lit cooling); riders exactly while their window is live; stride keeps counting with `+` dropped after the spend; empower/IMP riders vanish on spend; MOM carries stacks plus grace; STR only above zero; Zero and classless render nothing.
- [x] Countdown formatting pinned at 10_000 / 9_999 / 0 ms, clamped at zero; elapsed past a window drops its row.
- [x] Labels read from the class definition and ability constants (no second copy to drift); badges from the fixed vocabulary.
- [x] `ChatLine.cooldown` reads `%.1fs`; any old whole-second pin updated.
- [x] `gradle test` green; the decision reads only the snapshot, elapsed and pure class data.

## Comments

Landed as `StatusHud` with a `Chip(badge, label, value, lit)` / `Row(List<Chip>)` model: a row is one
line of chips, the lane chip first and its riders after. Stride and Rend-empower riders are bare
countdown chips (no badge exists for them in the fixed vocabulary); the stride `+` prefixes the
countdown and drops at the spend while the rider counts on. The outcome chip draws initials derived
from the outcome's own label; **Vigor joins Zero in drawing no chip** — its heal lands at fire and
nothing rides its window, and the spec's initial list (E/W/H/J/B) omits it — noted here in case a
reviewer expects V. `RotateAbility.Stance` gained `label()` so the stance row and the `Gladius!`/`Scutum!`
chat lines share one copy. MOM's label comes from the passive definition's name and STR's from
`CritRule.STREAK`; countdowns are `%.1fs` Locale.ROOT, clamped at zero. `ChatLine.cooldown` switched to
`%.1fs` (the parallel lane's identical change should merge cleanly) and `RushAbilityTest`'s pin now reads
`Rush: 15.0s`. Verified: `gradle test`, 216 tests green (16 new `StatusHudTest`s).

