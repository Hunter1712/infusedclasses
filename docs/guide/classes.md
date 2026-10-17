# Classes

Three playable classes. The picker shows one focused card per page with the full kit printed on the card — nothing hides behind hover. A new class definition adds its own page with no layout change.

## Juggernaut — Frontline bruiser

Rush engage plus Rend execute. No max-health bonus (vanilla 20 HP): Vigor rewards staying topped, Momentum rewards staying on target.

**Actives**

- **Rush** (`R`): cleanses every active harmful effect, quickens the player (+0.02 move speed) for 5s (the Rush window), and arms a one-shot bonus so the next full-swing melee hit inside the window earns +5 Momentum. Own 25s lane. Fires with zero stacks at full HP — engaging topped-off is the normal case, not a reject. Rejected presses leave the lane, window and bonus untouched. Cast fanfare: wind-burst whoosh.
- **Rend** (`V`): arms the Rend window — the next melee hit landed within 6s deals 0.5 HP per Momentum stack bonus damage (10 HP at the full 20) — and consumes every Momentum stack plus the armor they granted up front. Own 60s lane. Zero stacks fizzles with a `No Momentum` line and arms nothing. An unswung window expires with the stacks already spent. Cast fanfare: anvil-land smash.

**Passives**

- **Momentum** (conditional passive, `on_hit_dealt`): full-swing landing hits earn stacks, max 20, `minecraft:armor` +0.4 per stack (max +8.0). 5s grace with no landed hit drops all stacks at once. Spam earns zero — only full swings bump. Transient: never saved.
- **Vigor** (HP-scaled pair, reads as one line): current HP maps to stepped buckets, 10% per step over 10 steps — +0.3 attack damage from -1.0 (full HP +2.0) plus +0.075 attack speed from -0.25 (full HP +0.5). Recomputed every tick from live HP, transient and never saved. Rend spends Momentum only and never touches Vigor.

## Legionary — Sword and shield

Stance trader with flip buffs and one oath ultimate. Carries no conditional passives and no HP-scaled pair: stances are transient auras, not stacks. Every Legionary enters in Scutum.

**Stances**

- **Gladius** (striking): `attack_damage` +1.0, `attack_speed` +0.3, melee taken x1.1. Glass reads as glass.
- **Scutum** (plate, the default): `armor` +3, `armor_toughness` +1, `movement_speed` about -6%, melee dealt x0.9. Plate reads as plate.

**Actives**

- **Rotate** (`R`): flips Gladius/Scutum any time off its 8s lock. Flipping to Gladius arms the Impetus next-hit buff; flipping to Scutum opens the Testudo catch. Announces the new stance, no cast word. Rejects arm nothing. The `R` bind is the Signature bind, shared by every class, so binds survive the class.
- **Impetus** (Gladius-flip buff): the next melee hit after flipping to Gladius deals x1.10, at any charge, one-shot, expiring 5s after the flip. Flipping away drops it with the stance that earned it. No lane.
- **Testudo** (Scutum-flip buff): melee taken pays x0.85 for 1.5s from the flip. Timed, never consumed: every hit inside the catch is discounted. No lane.
- **Aequilibrium** (`V`, oath ultimate): +1.0 attack damage + +3.0 armor for 6s, in either stance, on the shared 50s oath lane. No deal/take mults, no downsides: the stance stays underneath and both layers count. Announces `Aequilibrium!` gold; rejects arm nothing. The `V` bind is the Ultimate bind, shared by every class.

## Gambler — Double or nothing

Wheel-and-dice class: Roulette spins stat bands and All In loads the crits, and the crit rule doubles as its passives. Carries no conditional passives and no HP-scaled pair: Streak is a transient counter, not stacks.

**Actives**

- **Roulette** (`R`): one spin rolls six bands — Edge x1.25 damage (25%), Ward +2 armor (25%), Haste +0.02 speed (25%), Jackpot all three stats (10%), Bust x0.75 melee (10%), Zero (5%) — the stat effects last 10s, and nothing touches HP. Own 25s lane. Fired spins announce their band, no cast word; rejects arm nothing. Zero is a dead band: fired, announced, and worth nothing. The `R` bind is the Signature bind, shared by every class. Cast fanfare: note-block pling.
- **All In** (`V`): crits deal x2.0 instead of x1.5 for 15s — the window lifts the crit multiplier and bends nothing else: betrayal keeps its band, and a crit still resets Streak. Own 75s lane. Announces `All In!` gold; rejects arm nothing. The `V` bind is the Ultimate bind, shared by every class. Cast fanfare: beacon swell.

**Passives** (the crit rule on the card)

- **Crooked Dice**: full-charge landed hits crit 20% at x1.5 and betray 5% at x0.5. The crit band locks at 95% from streak 5; betrayal stays 5% forever.
- **Streak**: a non-crit full hit loads +15 percentage points of crit (a betrayal counts); any crit resets it to the 20% base. Transient like every earned count: wiped by class switch, respawn and relog.

## Shared melee rules

- **Bonus curve**: mod hook bonus damage scales quadratically with swing charge (half charge pays a quarter, spam pays chips). Base weapon damage never routes here.
- **Stack gate**: conditional triggers fire only at full charge, so spam earns zero stacks and timed swings earn one. Rush bonus rides the same gate.
