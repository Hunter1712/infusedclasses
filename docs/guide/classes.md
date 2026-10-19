# Classes

Three playable classes. The picker shows one focused card per page with the full kit printed on the card — nothing hides behind hover. A new class definition adds its own page with no layout change. Firing any ability plays the one cast fanfare, the evoker's cast spell; Rotate announces its stance without it.

## Juggernaut — Frontline bruiser

One bar, two ways to spend it. Burden makes every swing heavier and slower; Momentum is what
moves under that weight. No max-health bonus (vanilla 20 HP).

**Passives**

- **Burden** (always on): x1.15 damage, -0.4 attack speed. The class reads heavy first.
- **Momentum** (conditional passive): full-swing hits dealt earn +1, and hits taken earn +1
  too, max 60. 5s quiet, then -1/s. Three checkpoints, read off the bar every tick: 15 Steady
  (+1 armor, -0.005 speed, +0.25 knockback resistance), 30 Braced (+3/-0.01/+0.5), 45 Rooted
  (+5/-0.015/+1.0). Past 45 stays Rooted; 45-60 is spend reserve. Transient: never saved.

**Actives**

- **Offload** (`R`): pays a flat 5 Momentum, then 5s of +0.2 attack speed and +0.01 move speed.
  Own 10s lane. Below 5 stacks fizzles with a `No Momentum` line and arms nothing. An unspent
  window owes nothing back.
- **Onslaught** (`V`): needs 10 Momentum; for 15s every landed hit deals x1.15 melee and every
  full swing spends 2 stacks. Nothing feeds the bar while it is live, and the window ends when
  its 15s runs out or the bar empties, whichever comes first. Own 60s lane. Below 10 stacks
  fizzles with a `No Momentum` line.

## Legionary — Sword and shield

Stance trader with flip buffs and one oath ultimate. Carries no conditional passives and no HP-scaled pair: stances are transient auras, not stacks. Every Legionary enters in Scutum.

**Stances**

- **Gladius** (striking): melee dealt x1.15, `attack_speed` +0.3, melee taken x1.2. Glass reads as glass.
- **Scutum** (plate, the default): `armor` +3, `armor_toughness` +1, `movement_speed` -0.005 flat, melee dealt x0.9. Plate reads as plate.

**Actives**

- **Rotate** (`R`): flips Gladius/Scutum any time off its 8s lock. Flipping to Gladius arms the Impetus next-hit buff; flipping to Scutum opens the Testudo catch. Announces the new stance, no cast word. Rejects arm nothing. The `R` bind is the Signature bind, shared by every class, so binds survive the class.
- **Impetus** (Gladius-flip buff): the next melee hit after flipping to Gladius deals x1.10, at any charge, one-shot, expiring 5s after the flip. Flipping away drops it with the stance that earned it. No lane.
- **Testudo** (Scutum-flip buff): melee taken pays x0.85 for 1.5s from the flip. Timed, never consumed: every hit inside the catch is discounted. No lane.
- **Aequilibrium** (`V`, oath ultimate): melee dealt x1.15, +3.0 armor for 6s, in either stance, on the shared 50s oath lane. No take mult, no downsides: the stance stays underneath and both layers count. Announces `Aequilibrium!` gold; rejects arm nothing. The `V` bind is the Ultimate bind, shared by every class.

## Gambler — Double or nothing

Wheel-and-dice class: Roulette spins stat bands and All In loads the crits, and the crit rule doubles as its passives. Carries no conditional passives and no HP-scaled pair: Streak is a transient counter, not stacks.

**Actives**

- **Roulette** (`R`): one spin rolls six bands — Edge x1.25 damage (25%), Ward +2 armor (25%), Haste +0.02 speed (25%), Jackpot all three stats (10%), Bust x0.75 melee (10%), Zero (5%) — the stat effects last 10s, and nothing touches HP. Own 25s lane. Fired spins announce their band, no cast word; rejects arm nothing. Zero is a dead band: fired, announced, and worth nothing. The `R` bind is the Signature bind, shared by every class.
- **All In** (`V`): crits deal x1.75 instead of x1.5 for 15s — the window lifts the crit multiplier and bends nothing else: betrayal keeps its band, and a crit still resets Streak. Own 75s lane. Announces `All In!` gold; rejects arm nothing. The `V` bind is the Ultimate bind, shared by every class.

**Passives** (the crit rule on the card)

- **Crooked Dice**: full-charge landed hits crit 15% at x1.5 and betray 5% at x0.5. The crit band locks at 95% from streak 6; betrayal stays 5% forever.
- **Streak**: a non-crit full hit loads +15 percentage points of crit (a betrayal counts); any crit resets it to the 15% base. Transient like every earned count: wiped by class switch, respawn and relog.

## Shared melee rules

- **Full swing**: only a `>= 1.0` charge earns stacks, rolls a crit or prices Onslaught. Spam
  earns zero and prices nothing, and a hit fully eaten by a raised shield earns nothing on
  either side.
