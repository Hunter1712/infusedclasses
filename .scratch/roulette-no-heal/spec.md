# Spec: Roulette re-cut — no healing

Status: done

## Problem Statement

The wheel heals. Two of Roulette's seven bands hand HP back for free — Vigor (20%, instant 3) and Jackpot (8%, instant 4 atop its three stats) — which makes the Gambler's free, 25s-lane spin double as a sustain button: pressing R to top up betrays the class's bet-and-lose identity, and a band whose whole payoff is a heal sits awkwardly beside stat bands the player has to weigh. It is also the only heal the mod hands out, so "can I press a button to get HP back" has exactly one answer and it is chance. Directive: remove all healing from Roulette.

## Solution

- **No heal path exists anywhere**: the Vigor band, `VIGOR_HEAL`, `JACKPOT_HEAL`, the `healHp` enum component and accessor, the applier's heal branch, and `RouletteApplier` itself are deleted. A fired spin announces and casts; nothing touches HP.
- **Six bands, the same wheel, odds re-cut ×1.25**: Edge 25% (×1.25 damage), Ward 25% (+2 armor), Haste 25% (+0.02 speed), Jackpot 10% (all three stats, no heal), Bust 10% (×0.75), Zero 5% (dead) — 100% filled, same order and rule set (10s window, 25s lane, free spin, Edge/Bust read by the melee plan, Ward/Haste through the window-aura path).
- **Copy regenerates from the numbers**: `One roll: Edge 25% x1.25, Ward 25% +2 armor, Haste 25% +0.02 speed, Jackpot 10% all, Bust 10% x0.75, Zero 5%; 10s (R, 25s).`

## User Stories

1. As a Gambler, I want the wheel to hand out stats only, so every band is a bet rather than a heal button.
2. As the mod's maintainer, I want healing gone at the root — no constants, no dead branch — so no later path can quietly bring it back.
3. As a player, I want no stale heal mechanics pinned by tests or described in docs.
4. As a picker reader, I want the wheel's copy to show the six-band odds and no heal text, so it matches what spinning does.
5. As a Gambler, I want the remaining bands, window and lane untouched, so the re-cut changes only what was asked.

## Implementation Decisions

- `RouletteAbility.Outcome`: delete `VIGOR`; band tops become EDGE 0.25, WARD 0.50, HASTE 0.75, JACKPOT 0.85, BUST 0.95, ZERO 1.00; drop the `healHp` component from the enum's constructor, field and accessor; delete `VIGOR_HEAL` and `JACKPOT_HEAL`. `percent()` keeps reading off the tops, so the odds copy regenerates; `statAuraFor` loses its VIGOR arm (Jackpot keeps both modifiers).
- `RouletteApplier` is deleted: with the heal gone its body is exactly the shared announce-and-cast every other ability calls directly from its receiver (All In, Aequilibrium do). The receiver's ROULETTE branch calls `AbilityApplier.announceAndCast(player, result.outcome().announcement(), result.outcome().fired(), RouletteAbility.CAST_SOUND, true)`.
- Javadocs that name the heal are updated: `RouletteAbility` ("Vigor and Jackpot heal at fire" goes), the `RouletteApplier` file goes, and the GLOSSARY's Ability applier sentence ("only Rush and Roulette keep their own adapters — Rush for the cleanse and the stride it lays, Roulette for the instant heal") loses its Roulette half.
- Nothing else moves: the 10s window, the outcome payload on the runtime, the 25s lane, the free spin, Jackpot's two stat modifiers, and the deal mults are untouched.

## Testing Decisions

- Update `RouletteAbilityTest`: band cuts at 0.249/0.25, 0.499/0.50, 0.749/0.75, 0.849/0.85, 0.949/0.95 and 1.0 → Zero; percent pins 25/25/25/10/10/5 with the 100 sum; the mults test loses every heal assertion (`goodAndBadBandsCarryTheirMultsAndHeals` becomes a mults-only test, VIGOR removed from the quiet-aura list); the desc pin takes the six-band text.
- No new applier tests: the deleted adapter had none (adapters are manually verified, house convention).
- `gradle test` green, and a grep over `src/` finds no `VIGOR_HEAL`, `JACKPOT_HEAL`, `healHp`, or `player.heal` inside the Roulette paths (the Juggernaut's `Vigor` HP-scaled passive is unrelated and stays).

## Out of Scope

- Other healing sources (there are none in the mod; `/heal`-style admin tools are not this mod's business).
- Balance changes to the remaining bands' magnitudes, the 10s window, the 25s lane, All In, or the dice.
- A replacement downside band, a shrunken Jackpot, or heal-as-cost gambits.

## Further Notes

- The ×1.25 re-cut is arithmetic, not tuning: the three 20% bands become 25% and the 8%/4% bands become 10%/5%, so six bands still fill 100%. If playtest wants new magnitudes, the same one-line constants move.
- ADR-0015 records the decision; the GLOSSARY's Roulette entry and any guide copy drop their heal sentences.
