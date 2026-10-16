# 01: Six-band re-cut, heal path deleted

Spec: `.scratch/roulette-no-heal/spec.md`

**What to build:** Spins hand out stats and nothing else — the heal path disappears rather than being disabled: the Vigor band, `VIGOR_HEAL`, `JACKPOT_HEAL`, the `healHp` component and accessor, the applier's heal branch and the `RouletteApplier` file itself all go. The wheel reads six bands at 25 / 25 / 25 / 10 / 10 / 5 (Jackpot all three stats, no heal), with the 10s window, 25s lane, free spin and deal mults untouched, and the desc regenerates from the new tops. Firing announces and casts directly, the way All In and Aequilibrium do.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Band cuts re-pinned (0.249/0.25, 0.499/0.50, 0.749/0.75, 0.849/0.85, 0.949/0.95, 1.0 → Zero); percents 25/25/25/10/10/5 summing 100.
- [x] Every heal assertion and the VIGOR aura arm deleted; the desc pin takes the six-band text.
- [x] `grep -E "VIGOR_HEAL|JACKPOT_HEAL|healHp" src/` clean (the Juggernaut's Vigor passive stays).
- [x] Naming javadocs updated; the glossary's Ability applier sentence loses "Roulette for the instant heal".
- [x] `gradle test` green.

## Comments

Landed: `VIGOR`, `VIGOR_HEAL`, `JACKPOT_HEAL`, the `healHp` component/accessor and the `RouletteApplier` file are gone; the six bands cut at 0.25/0.50/0.75/0.85/0.95/1.00 and the desc regenerates six-band text; the ROULETTE receiver calls `AbilityApplier.announceAndCast(player, result.outcome().announcement(), result.outcome().fired(), RouletteAbility.CAST_SOUND, true)` directly. `RouletteAbilityTest` re-pinned: boundary cuts 0.249/0.25, 0.499/0.50, 0.749/0.75, 0.849/0.85, 0.949/0.95 and 1.0 → Zero; percent pins 25/25/25/10/10/5 with the 100 sum; `goodAndBadBandsCarryTheirMults` is mults-only; VIGOR out of the quiet-aura list. `grep -E "VIGOR_HEAL|JACKPOT_HEAL|healHp|RouletteApplier" src/` clean. `gradle test` green (181 tests, 23 classes). No deviations.
