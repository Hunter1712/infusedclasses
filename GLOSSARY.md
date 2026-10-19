# Infused Classes

Class selection and what a chosen class does for a player: the classes on offer, the choice a player
makes, the passives and ability that follow from it, and the picker screen that offers it.

The code anchor is given next to each term where one exists, so the vocabulary can be traced to the
module that carries it.

## Language

### Deciding and applying

**Decide-then-apply**:
Every gameplay or presentation question is answered twice: first by a decision that takes data and
returns a description of what should happen, then by an adapter that actually applies it. A decision
never touches a player, level, attribute instance or graphics object, so it can be asserted in plain
JUnit; the adapter owns every one of those.
_Avoid_: the seam, the pure/live split (both too vague on their own), effect system

**Passive effect** (`PassiveEffect`):
The decision of which attribute modifier an HP-scaled bucket is worth, as an attribute, a
modifier id and an amount, plus the write/remove/skip plan for it. Bucket zero means the passive
contributes nothing, and that answer is an absent modifier rather than a zero-valued one; the
bar's own rows are the `ConditionalPassive` tiers, not this.
_Avoid_: passive bonus, armor bonus, stack effect (the decision is not the thing on the player)

**Passive applier** (`PassiveApplier`):
The adapter that makes the player's attributes agree with the passive effect. It writes only when
the applied value differs from the wanted one, following one reconcile-by-modifier-id pass:
write every wanted row, then remove every id in the family's full set that the wanted set does
not name. Each tick runs the bleed (`PlayerRuntime.decay`), the Momentum rows for the bar's
current count (`Momentum.forStacks`), each always-on chassis row (`Burden`), the HP-scaled
buckets, then the window-aura families. A class switch removes what the old class wrote but the
new one does not carry — the pure `cleanupFor(oldDef, newDef)` answers every modifier of that
list, keyed by id, and `onClassChanged` only removes what it answered — then drops the old
stacks and resets the lanes. The hit settle belongs to the melee plan (see `Melee plan`).
_Avoid_: passive system, stat updater

**Window aura family** (`PassiveApplier.WindowFamily`):
One window ability's aura group: the marker ability that owns it, every modifier the family can
write, and `wanted(state)`, the pure decision of what the ability's live windows earn. The tick
reconciles each family once after the bar and bucket passes — a held marker writes what is wanted
and removes the rest, so the write and remove sides cannot drift; an absent marker sweeps
everything the family could write. The three families are the Legionary stance plus its
Aequilibrium window, the wheel's live spin, and Offload's loosened pair.
_Avoid_: aura group, modifier set, buff family

**Ability outcome** (`AbilityOutcome`):
The shared envelope every ability decision returns: whether the press fired and the
announcement that goes with it. `fired`, `rejected` (silent — a wrong class and a lane
still cooling down are the same shape) and `fizzled` (says its line, no cast) are the
three factories. An ability with a payload wraps the envelope in its
own `Result` record instead of widening it: Rotate's landed stance, Roulette's band.
`trigger` gates read-only; `fire` gates and arms the transient runtime's cooldown together, so no
caller can fire without arming, and rejected presses leave the cooldown untouched.
_Avoid_: applied outcome, ability result (both name the wrapper), buff

**Ability applier** (`AbilityApplier`):
The shared adapter seams every ability outcome and class-choice outcome passes through: the announcement
on the action bar (abilities, rotate) or chat (class-choice), plus the one cast fanfare
(`CAST_SOUND`, played by `playCast`) for fired abilities. The fanfare id lives here beside its
play path; effect ids resolve at their single use sites — Offload's pair in its own module
(`OffloadAbility.HASTE_ID`/`SPEED_ID`) — never behind shared
lookups with one caller each. A result with nothing to grant and nothing
to say — a wrong-class, on-cooldown or no-stacks press — lands nothing.
A fired-or-fizzle press lands through `announceAndCast` alone: the Juggernaut pair, Roulette
and the two ultimates all arm inside their decision and route their fanfare through the shared
path. Class-choice rejections and accepts route through `announce` with the `actionBar` flag.
_Avoid_: ability system, effect handler

**Ability roster** (`AbilityRoster`):
The one table of the abilities the mod fires: a row per ability carrying its `Lane`, the id
that gates it, its Title Case word (the ability module's own `NAME`) and the rider rules that
draw beside its lane. Both lane reads derive from the row's lane — the `State` read
server-side, the payload read client-side (ADR-0023) — so a row cannot draw one lane and count
down another. `/class info` draws a lane line per row the class carries, the HUD's rows
decision draws each lane line, word and countdown and riders included, from the row, and
`BindMorph` resolves what a reused slot means by walking the class's own actives through each
target's `ability()` (the first that answers wins) and returns the fired target's own row
(`shape(def)`), so a word, a lane read, a rider rule or a bind cannot drift between the
consumers. The ultimate is the class's own ultimate ability, never lent from the signature row
(ADR-0026) — a class carrying neither slot ability draws no slot lines and falls back to Offload
and Onslaught. A new ability adds a row; a test
pins that every registered active id has one.
_Avoid_: ability list, lane table, ability registry

**Gate-and-arm**:
The shape every ability firing shares, per module: `trigger` gates read-only, `fire`
gates and arms the transient runtime's lane together, so no caller can fire without
arming and a rejected press leaves the lane untouched. The arm reads the same lane and
`now` the trigger just checked, so it cannot fail. Abilities keep their own numbers,
chat lines and windows.
_Avoid_: fire helper, trigger wrapper

**Chat line** (`ChatLine`):
The text and the chat style a decision returns for the server entry to send verbatim.
Onslaught's fire line is `Onslaught!` in red, Offload's is `Offload!` in yellow. Fire lines and fizzles
speak only what happened: a press on cooldown says nothing, so `ChatLine.cooldown`
(`%.1fs`, one decimal) stays the `/class info` lane shape (ADR-0017, whose silence
carried at ADR-0019). Its wording, rounding and locale are decided with the ability (or class-choice outcome),
not formatted by whoever sends it. Class-choice lines use the same seam: `unknownOrLocked()` (red, chat)
for a rejected pick, and `classPicked(displayName)` (gray, chat) for an accepted switch.
_Avoid_: message, notification, cooldown echo

**Picker copy** (`ConditionalPassive.fullDesc`):
The decision of what a conditional passive's picker copy says, generated from its own numbers so it
cannot drift from behaviour. Every detail line shows name plus short copy
(`Land a hit: +1. Take a hit: +1. Max 60. 5s no feed: -1/s.`).
Copy stays short on purpose: every active desc holds at most 200 chars, pinned by
test, so full kits fit the focused card with little scroll.
Both triggers the mod emits are described as the event they are ("Land a hit" for
`on_hit_dealt`, "Take a hit" for `on_hit_taken`); a trigger carries its own picker
word (`earn`), so a further trigger cannot be spelled inert. Whole numbers read
as `1`, not `1.0`.
_Avoid_: description, lore text

### Classes, passives and stacks

**Class** (`ClassDefinition`):
A playable archetype, registered once with its id, display name, passives, always-on rows,
HP-scaled passives, accent colour, tagline, the actives list (one row per fired active: its ability id, name and
picker description) plus the flip-buff list (one `FlipBuff` row per trigger-fired
active: name plus description, shown under `Passives`). A class is data: it carries no player and no state of its own. The first passive on a
trigger is found with `passiveForTrigger`, never by list position. Juggernaut, Legionary and Gambler are the three
playable classes today: `Juggernaut`, Offload (`infusedclasses:offload`) plus Onslaught
(`infusedclasses:onslaught`), accent `0xFFE74C3C`,
tagline `Frontline bruiser`, actives `Offload` and `Onslaught`, with picker copy
generated from the ability constants, so the numbers
cannot drift from behaviour; `Legionary` (see below), Rotate plus the Impetus/Testudo
flip buffs and the Aequilibrium oath ultimate, accent `0xFFB87333`, tagline `Sword and shield`;
`Gambler` (see below), Roulette plus All In and a crit rule that doubles as its
passives, accent `0xFFD4AF37`, tagline `Double or nothing`.
Juggernaut carries no max-health bonus (vanilla 20 HP): Burden is the heavy chassis and
Momentum is the one bar the kit earns and spends.
_Avoid_: character class definition, build

**Locked class**:
No locked classes exist: the definition carries no locked flag, the picker only
ever shows playable classes, and nothing rejects by lock. The word survives only
in the rejection line (`Unknown or locked class`).
_Avoid_: unavailable class, future class, hidden class

**Active** (`ClassDefinition.ActiveAbility`):
One playable active on a class card: its ability id, name and picker description in one row.
The id is what `hasAbility` reads, so a card row and its gate cannot drift apart. A class
carries one per active — Juggernaut carries Offload and Onslaught — so names never join with `+`.
_Avoid_: ability (ambiguous with the gating id), spell

**Flip buff** (`ClassDefinition.FlipBuff`):
One trigger-fired Legionary buff on a class card: name plus picker description, shown under
`Passives` because no key presses it. Never gated: it carries no ability id and nothing reads
`hasAbility` for it.
_Avoid_: passive (it is not stack-driven), toggle

**Always-on passive** (`ClassDefinition.AlwaysOn`):
One chassis row that is written while the class is held and never earned: the name, the picker
description, the attribute modifiers the tick applier writes, and the melee deal mult the melee
plan reads (`dealMult`, 1.0 when the row carries none; `ClassDefinition.chassisDealMult` is the
product over the rows). The written half and the read half share the row so the card copy, the
tick and the plan cannot drift. `Burden` is the only carrier: swing penalty written, x1.15 read.
_Avoid_: unconditional buff, base stat

**Conditional passive** (`ConditionalPassive`):
A passive that earns stacks when a named trigger fires, rather than always being on. It carries its
own numbers — the earns list (a trigger plus its amount each), stack cap, attribute tiers and grace
period — and its picker copy is generated from those numbers so the copy cannot claim something
the behaviour does not do. Momentum, the Juggernaut's one bar, is earned by both `on_hit_dealt`
and `on_hit_taken` (+1 each), caps at 60, bleeds 1/s after 5s quiet, and reads three checkpoints:
15 Steady (+1 armor, -0.005 speed, +0.25 knockback resistance), 30 Braced (+3/-0.01/+0.5) and
45 Rooted (+5/-0.015/+1.0); past 45 the bar stays Rooted, and the reserve above 45 is the actives'
spend headroom.
_Avoid_: buff, stackable stat

**Burden** (`Burden`):
The Juggernaut's always-on chassis: melee dealt x1.15 (`DEAL_MULT`) and -0.4 attack speed
(`infusedclasses:burden_swing`), written every tick while classed and never earned. The heavier
hit is a deal mult the melee plan reads (see `Always-on passive`), never a written attribute. The
class reads heavy first; Momentum is what moves under that weight.
_Avoid_: rage, frenzy stacks

**HP-scaled passive** (`HpScaledPassive`):
The shared bucket machinery: current HP maps to ten stepped buckets, each a fixed bonus over a
floor, recomputed every tick and transient. No class registers one today — Vigor retired with
ADR-0027 — but the module and the applier's bucket pass remain for the next kit that wants one.
_Avoid_: rage, frenzy stacks

**Stack**:
One earned increment of a conditional passive, counted per player and capped at that passive's
maximum. Stacks are keyed by the passive definition, never by its name, so a passive is addressed
by what it is rather than by what it is called. Past the grace the bar bleeds one stack
per second, and stacks are transient: they do not survive relog or respawn.
_Avoid_: charge, level, rank

**Cooldown** (`Lane`, `Cooldowns`):
The wait after an ability fires, tracked per player, also transient. `Lane` is the one enum
of the six windows — Offload 10s, Onslaught 60s, Rotate 8s, the Legionary shared oath 50s, the
Gambler's Roulette 25s and All In 75s — and each value owns its own length
(`cooldownMs()`), so a seventh window is one value plus its arm site. `Cooldowns` is the
lane-keyed `use(lane, now)` / `remaining(lane, now)` / `reset()` triad: `use` fires only when
that lane is ready and reports it, `remaining` reports the milliseconds left, 0 when ready,
and `reset` readies every lane again for class switch and respawn. The window rides the same
lane on the transient runtime: a fired press arms the lane and stamps the window together
(`PlayerRuntime.arm`; `rotate` and `roulette` arm the two landings that carry more), the
state reads each lane as one `LaneRead` (cooldown, window remainder, one-shot flag), and
switch and respawn clear all of it. Offload's window, Testudo, Aequilibrium, the Roulette
outcome and All In are timed (peeked while live, never consumed); Onslaught's lasts the
window or its bar, whichever empties first, and Impetus is the one-shot (peeked, spent on the
next landed hit).
_Avoid_: timer

**Offload** (`OffloadAbility`):
The Juggernaut's spender on `R`: pays a flat 5 Momentum (`COST_STACKS`), arms its own 10s
lane and stamps a 5s window (`WINDOW_TICKS`) of looser swings and a quicker step
(+0.2 attack speed `infusedclasses:offload_haste`, +0.01 move speed
`infusedclasses:offload_speed`). Below the price it fizzles with a `No Momentum` line and
arms nothing; a wrong-class press and one during the lane's cooldown reject silently.
Nothing is owed back when the window goes unspent: the stacks are already gone. Decided in
the trigger/fire gate-and-arm shape. A stat, never a vanilla effect: no effect icon, no
effect dependency.
Like every cast, firing plays the shared fanfare (see `Spellcast`).
_Avoid_: dash, charge

**Spellcast** (`AbilityApplier.CAST_SOUND`):
The one fanfare shared by every active: firing any ability plays the same note-block pling
(through `AbilityApplier.playCast`), so landing a cast is one learnable rule; fizzles and
rejects cost nothing and stay silent. The id and its single play path live here so appliers
never duplicate them. Rotate carries no cast sound and announces without fanfare.
_Avoid_: cast time, wind-up

**Onslaught** (`OnslaughtAbility`):
The Juggernaut's ultimate on `V`: needs 10 Momentum (`MIN_STACKS`), arms its own 60s lane
and a 15s window (`WINDOW_TICKS`). Inside it every landed hit deals x1.15 melee (`DEAL_MULT`)
and each full-swing hit spends 2 stacks (`COST_PER_HIT`), and nothing feeds the bar — dealt or
taken. The window lasts the bar and the window, whichever empties first: a spend down to
zero ends it there, and a bleed does too on the tick that reads the empty bar. Below 10
stacks the press fizzles with a `No Momentum` line and arms nothing. Decided in the
trigger/fire gate-and-arm shape; the melee plan pays the mult on any landed hit while the
window is live and prices full swings only, so a hit eaten by a shield never pays. Spam inside
the window pays the mult without a price: the bar drains only on full swings and the bleed.
Like every cast, firing plays the shared fanfare (see `Spellcast`).
_Avoid_: execute, heal button, second ability

**Legionary** (`ClassRegistry`):
The second playable class: `infusedclasses:legionary`, display `Legionary`,
accent bronze `0xFFB87333`, tagline `Sword and shield`, actives Rotate plus the
Impetus/Testudo flip buffs and the Aequilibrium oath ultimate. It carries no conditional passives and
no HP-scaled pair: its stances are transient auras on the runtime (see below),
not stacks, so the card groups the flip buffs under `Passives` and the play lives
in the fired actives. Every Legionary enters in `Scutum`.
_Avoid_: roman, tank class

**Gladius**:
The Legionary's striking stance: melee dealt x1.15 (`GLADIUS_DEAL_MULT`),
`attack_speed` +0.3 (`infusedclasses:gladius_alacrity`), melee taken x1.2. Glass reads as glass.
_Avoid_: dps stance, sword form

**Scutum**:
The Legionary's plate stance and the default: `armor` +3
(`infusedclasses:scutum_plate`), `armor_toughness` +1
(`infusedclasses:scutum_toughness`), `movement_speed` -0.005 flat
(`infusedclasses:scutum_weight`), melee dealt x0.9. Plate reads as plate.
_Avoid_: tank stance, shield form

**Rotate** (`RotateAbility`):
The Legionary's stance toggle on `R`: flips `Gladius`/`Scutum` any time off the
rotate lane's 8s lock (`Lane.ROTATE`), no build-up, no stacks. Picking a stance applies its aura
at once; leaving removes it at once. Flipping to `Gladius` arms the Impetus
next-hit buff, flipping to `Scutum` opens the Testudo catch. Firing announces the new stance (`Gladius!`
red / `Scutum!` gray) with no cast word; rejects arm nothing. The `R` bind is
the Signature bind, shared by every class, so binds survive the class.
Decided in the trigger/fire gate-and-arm shape.
_Avoid_: swap stance, transform

**Impetus** (`RotateAbility`):
The Legionary's Gladius-flip buff: the next melee hit after flipping to `Gladius`
deals x1.10 (`SWITCH_DEAL_MULT`), at any charge, one-shot through the
peek/consume pair, expiring 5s after the flip (`SWITCH_DEAL_MS`).
It rides the rotate lane's window: the flip arms it, and the state reads that lane's one-shot
only under a Gladius landing, so no second record exists. Flipping to `Scutum` never
carries it.
_Avoid_: enrage, burst

**Testudo** (`RotateAbility`):
The Legionary's Scutum-flip buff: melee taken pays x0.85 (`SWITCH_TAKE_MULT`)
for 1.5s from the flip (`SWITCH_TAKE_MS`), riding the same rotate lane's window.
Timed, never consumed: every hit inside the catch is discounted.
_Avoid_: block, turtle

**Aequilibrium** (`AequilibriumAbility`):
The Legionary's clean oath ultimate on `V`: melee dealt x1.15 (`DEAL_MULT`) + +3.0 armor
(`infusedclasses:aequilibrium_plate`) for 6s (`WINDOW_TICKS`), in either stance,
on the shared 50s oath lane (`Lane.OATH`). The edge is a deal mult the melee plan reads
while the window is live (never a written attribute); the plate rides the stance applier.
No take mult, no knockback immunity, no downside: the stance stays
underneath and both layers count. Firing announces `Aequilibrium!` gold;
rejects arm nothing. Decided in the trigger/fire gate-and-arm shape.
_Avoid_: ultimate, finisher

**Gambler** (`ClassRegistry`):
The third playable class: `infusedclasses:gambler`, display `Gambler`, accent
gold `0xFFD4AF37`, tagline `Double or nothing`, actives Roulette plus All In,
and a crit rule (see below) that doubles as its passives: Crooked Dice and
Streak render as its `Passives` lines, and the wheel is its utility.
_Avoid_: casino class, luck build

**Roulette** (`RouletteAbility`):
The Gambler's free spin on `R`, 25s lane: one roll over six bands — Edge
×1.25 damage (25%), Ward +2 armor (25%), Haste +0.02 speed (25%), Jackpot
all three stats (10%), Bust ×0.75 melee (10%), Zero (5%) — the stat
effects last 10s, and nothing touches HP. Fired spins announce their
outcome; rejects arm nothing. Ward and Haste stat the player through the
window-modifier path; Edge, Jackpot and Bust deal mults are read by the
melee plan.
_Avoid_: wheel button, gamble skill

**All In** (`AllInAbility`):
The Gambler's ultimate on `V`, 75s lane: crits deal ×1.75 instead of ×1.5 for
15s. The dice rules never bend: betrayal, Streak resets and every band stay as
they are.
_Avoid_: double damage, ultimate form

**Swing charge** (`SwingChargeAccess`):
The charge vanilla read for the swing being settled, captured at the one place
each melee damage route passes: just above the attack-strength ticker reset in
`onAttack` for normal swings, and at the head of `stabAttack` for spear thrusts
and mace smashes, which damage inside the stab and reach `onAttack` after the
damage (spears) or never (mace). The damage hook runs after all of these, so the
capture is the hook's only source for it.
_Avoid_: attack cooldown, swing timer

**Full swing** (`FullSwing`):
The one definition of a full swing, shared by the stack feed, the crit roll and the
Onslaught price: exact `>= 1.0F`, so overcharge counts, half swings and spam earn
nothing, roll nothing and price nothing. The timer scale itself is not an ability
concern: full-charge weapon damage is vanilla's (`baseDamageScaleFactor`, 0.2 + 0.8 x
charge squared), and the mod never touches it. (The charged bonus curve retired with
ADR-0028; `bonusFor` had already lost its last caller.)
_Avoid_: damage falloff, charge multiplier

**Melee plan** (`MeleePlan`):
The decision of what one melee hit earns and settles, taken from definitions,
transient state and what the hit landed: whether the swing earns stacks (full
swings only, spam earns zero; a hit that took no damage earns nothing and reads
no state at all), the Onslaught price (full swings only, 2 stacks, inside a live window),
the chassis deal mult (every always-on row's `dealMult`, any charge), the stance deal and take
mults (1.0 for anyone else), the wheel's live deal mult, the oath window's deal mult and
the Impetus flip buff (pending only, spent once, any charge). `planHit` is the pure half:
it decides the whole hit in field order — bump (suppressed while Onslaught is live),
Onslaught price flag, Impetus spend, dice verdict, combined mult (chassis x stance x
wheel x oath x onslaught x take x dice) — peeking
only. `resolve` plans through it and settles the runtime movements in the same order at the
same moment, returning `Landed(verdict, combined)` for the entry to land, so the
pairing the plan saw is the pairing that lands and empty hits never touch a window; the
attacker branch (`settleAttackerHit`) lands exactly what the resolve returned (cue,
combined mult) so the hook cannot reorder or half-apply it; Gladius plus
Impetus reads x1.1 deal, Scutum plus Testudo x0.85 take, a crit x1.25 deal, named once in the
plan rather than emerging in the entry. `resolveTaken` feeds the victim's `on_hit_taken`
+1, suppressed while its Onslaught window is live and only for a landed hit; the server
entry maps, adapters apply; the victim runtime arrives lazily, so a classless victim costs
no transient state. Owns every ability-id check the hook needs, so the entry never compares
ability ids to gate rewards.
_Avoid_: damage handler, hook logic

**Damage settle** (`DamageSettle`):
The single guarded hurt for the whole mod plus the mult settle every melee
branch funnels through. Over 1.0 lands extra hurt (dead targets deal nothing); under
1.0 heals back the discount, so the net hit reads as landed times mult; exactly 1.0
touches nothing. The base it scales is what the hit landed after shield and modifier
reductions, so a blocked hit settles nothing. Every attacker-side mult — chassis,
stance, wheel, oath, window, dice — arrives as the one combined mult, so the
settle is the single landing point.
Vanilla drops any same-tick
follow-up that does not beat `lastHurt`, so the extra goes in with that memory
cleared and leaves the memory at the hit plus the bonus: the step lands silently
with armor, death
protection and kill credit intact, and later hits still compare against the
combined swing.
_Avoid_: damage handler, hurt helper

**Hurt guard** (`DamageSettle.isResolving`):
The one re-entrancy flag for the mod: settled hurts re-enter the damage hook on the
same thread, and the hook grants nothing twice while it is set. Replaces the twin
`resolvingBonus` / `resolvingEmpower` flags.
_Avoid_: recursion flag, lock

**Stance applier** (`PassiveApplier`):
The adapter that makes the player's attributes agree with the Legionary stance: only
the current stance's modifiers sit on the player, the other stance's come off, same
write-only-on-change shape as the always-on and bucket passes. Aequilibrium plate rides the oath
window while live (its edge is a deal mult, never written). Both sides iterate the stance's single aura list (`RotateAbility.auraFor`,
`AequilibriumAbility.windowModifiers`), so write and remove cannot drift. The Roulette
outcome window's stat auras are the wheel's own family (`rouletteWanted`) on the same tick,
swept when the class leaves. Leaving the class
takes them all off through `onClassChanged`, which removes exactly `cleanupFor`'s answer.
_Avoid_: stance system, aura updater

**Bind morph** (`BindMorph`):
The decision of what a slot bind means for a class: the signature slot (`R`)
arrives as the rotate intent for Legionaries, the roulette intent for Gamblers,
and Offload for everyone else; the ultimate slot (`V`) arrives as the
oath ultimate for Legionaries in any stance, All In for Gamblers, and Onslaught for
everyone else. One ultimate per class, never lent from the signature row (ADR-0026): each
target names the ability it fires, so one walk of the class's own actives answers both the
slot and the row that draws it (`shape(def)`): the keys fire from the same walk, and the
HUD's slot lines read the same rows.
The intent receivers wire — sharing one `press` prologue (class read, classless bail, runtime
lookup) — this decides.
_Avoid_: key routing, bind table

**Stack gate**:
The other half of the spam fix: the attacker branch reads `attackStrengthScale`
once per swing and feeds conditional triggers only at full charge
(`MeleePlan.planHit` over `FullSwing.isFull`), so spam
earns zero stacks and timed swings earn one. Onslaught prices on the same gate.
Crit rolls and Streak movement ride the same gate:
half swings never roll and never load the pocket.
`Juggernaut Momentum` grace reads 5s to cover the swing timer
under the gate.
_Avoid_: swing check, charge requirement

### Crits and chance

**Crit roll** (`CritRoll`):
The decision of one full-charge landed hit's verdict — crit, betrayal or
normal — from the class's crit rule, the current Streak, the All In window,
the swing charge and a roll the caller drew. The universal baseline is 10%
crit at ×1.25, full-charge hits only, no betrayal and no Streak movement; a
class override replaces it, and a classless player reads as the baseline.
Vanilla crits are retired: the mixin cancels `canCriticalAttack` for every
player, so crits are a mod rule.
_Avoid_: crit check, damage roll

**Crit rule** (`CritRule`):
A class's dice data: crit chance and mult, betrayal chance and mult, Streak
step. One record; a class without one uses the universal baseline. The
Gambler's `Crooked Dice` and `Streak` card lines are generated from it, so the
Streak reset target cannot drift from the chance base.
_Avoid_: crit config, dice passive

**Crooked Dice**:
The Gambler's crit rule: 15% crit at ×1.5, 5% betrayal at ×0.5, +15
percentage points of crit per non-crit full hit. The crit band locks at 95%
from streak 6; betrayal stays 5% forever.
_Avoid_: luck passive

**Betrayal**:
The fixed band at the foot of the Gambler's crit roll: 5% of full-charge hits
deal ×0.5. It never grows and is never removable, and it still feeds Streak.
_Avoid_: backlash, mishap

**Streak**:
The Gambler's loaded pocket: +15 percentage points of crit chance per non-crit
full hit (a betrayal counts as one); any crit resets it to base. The crit band
stops growing at 95% from streak 6. A transient counter like every earned
count: wiped by class switch, respawn and relog.
_Avoid_: charge, counter

### Per-player state

**Class choice**:
The single class a player has selected, or none. It is the only per-player fact that is saved, and it
survives relog and respawn. Choosing an unknown or missing class is rejected and leaves the
previous choice untouched; a bad or unknown id reads back as none.
_Avoid_: selection (ambiguous with the picker's selection), equipped class

**Class choice module** (`PlayerClasses`):
The module that owns the class choice and nothing else: the id, validation, and the round-trip that
saves it to player data under `InfusedClass`. It reads no clock and holds no transient state, so what
it does is assertable without a game: `writeId`/`readId` for the save string, `trySetClass` for
validated picks (unknown and missing ids yield null), `currentDefinition` for the pure
current class, `copyFrom` for the respawn copy, which copies the choice and nothing else.
_Avoid_: player classes, player state (both still suggest transient state; that is the transient
runtime's job)

**Choice access** (`PlayerClassAccess`, `mixin/PlayerMixin`):
The one thing the mod needs from a player: the class it chose. The mixin implements the access on
every player and holds one choice slot per player, saved with player data (`ValueInput`/
`ValueOutput`), so the choice — and only the choice — lives on the player. The cast is the only way
for the server entry to reach a field a mixin added.
_Avoid_: player hook, mixin state

**Class switch** (`ClassSwitch`):
The one adapter path behind both switch entries: validate the pick in the choice module,
clean what the old class earned (`PassiveApplier.cleanupFor`'s answer, removed by
`onClassChanged`) and announce the outcome,
rejection included; it returns the new definition, or null when the pick was rejected.
Callers keep only their own extra — the picker's confirm packet, the command's return
code — so the two entries cannot sequence the switch differently.
_Avoid_: class change, switch handler

**Transient runtime** (`PlayerRuntime`):
The per-player stacks, cooldowns, ability windows and the Gambler's Streak
counter that last only as long as the session. It is handed the class they
belong to and the current time on each call, so it holds neither a clock nor a reference to anyone's
choice, and it is owned per player by the server entry rather than stored on the player. Stack
storage (counts, last-feed stamps and the next-loss clock in definition-keyed maps) lives in `PassiveStacks`:
`stacks` reads one passive, `onTrigger` bumps
every passive on the class whose trigger matches (unknown triggers, a missing class and trigger-less
classes are inert, each bump re-stamps grace), `decay` bleeds every passive past its own grace
and catches up whole seconds in one step, and `spendStacks` pays a price without re-stamping.
`clear` drops one class's
stacks; cooldowns and windows are lane-keyed (`Lane`): the one
`arm`/`rotate`/`roulette`/`consumeBonus` set arms both together, `endWindow` ends a window
early, only an ability `fire` calls the arm set, and `resetAll` readies every lane for the
adapter's switch and
respawn. Each ability names its window constant once, at its arm site; reads take only the
clock. Callers and tests
cross this module's seam; the stacks module is its implementation.
_Avoid_: player state, live state, runtime (too generic on its own)

**Runtime state** (`PlayerRuntime.State`):
The whole transient read at one clock: a `LaneRead` per lane (its cooldown, its window
remainder, its one-shot flag), the flip target, the stance and the
Streak, taken in one `PlayerRuntime.state(now)` call. It is the one read every status path
crosses — the snapshot builder, `/class info`, the melee plan, the ability fires and the
passive applier — so lane and window semantics cannot be re-stated per caller; the per-window
read verbs are gone, while arms, consumes, movement verbs and the passive-stack verbs stay
verbs.
_Avoid_: runtime read, state object, snapshot

**Transient runtime store** (`PlayerRuntimes`):
The one module that owns the transient runtime's lifetime: one per UUID, created on first use
(`runtime`), dropped on logout (`remove`), empty again after a respawn (`respawn`, which removes
before creating because both bodies share one UUID). Assertable with UUIDs alone, no player needed.
_Avoid_: runtime map, session cache

### Presentation

**Card line** (`ClassPresentation.CardLine`):
One line of a class card's body, carrying its name plus optional description, the section it belongs to, and the colour and
weight it draws with. Full text derives via `text()`, and the tone boundary via `descStart()`: where the description starts
in the joined text, derived from the join, never searched.
_Avoid_: row, string, label

**Line kind** (`ClassPresentation.LineKind`):
Which section a card line belongs to: a passive or ability header, or a passive or ability item.
Kind is what decides style; a line's text never does.
_Avoid_: type, category

**Line style** (`ClassPresentation.LineStyle`):
The colour and boldness a line draws with, decided by its line kind alone: headers white bold
(`0xFFFFFFFF`), passive items light gray (`0xFFBDBDBD`), ability items blue (`0xFF7ED6FF`).
Continuation lines draw with the style of their parent item kind.
_Avoid_: format, theme

**Card lines** (`ClassPresentation.cardLinesFor`):
The detail lines a focused card shows. A class shows a `Passives` header plus one
name-plus-description line per passive (HP-scaled passives
sharing a group read as one line with joined descriptions) plus one per flip buff plus
the generated `Crooked Dice` and `Streak` lines for a class carrying dice,
or the
header plus `No passives yet` when it has none, and an `Active` header plus one
name-plus-description line per fired active. Names draw bright, descriptions dimmed.
Nothing hides behind hover.
_Avoid_: card text

**Picker subtitle** (`ClassPresentation.subtitle`):
The line under the picker title: `Your choice shapes your ability`.
_Avoid_: header

**Card accent** (`ClassPresentation.cardFor`):
The colour of a card's top strip: the class's own accent.
_Avoid_: highlight

**Picker button** (`ClassSelectScreen`):
The `Select` control every focused card carries: always that label and always
active, since every visible page is playable. Its rect comes from the card layout's button math, never the screen's.
_Avoid_: select button

**Card text** (`ClassPresentation.CardText`, `cardFor`):
The card content: display name, tagline, detail lines (with kinds and styles) and accent colour.
Frame and body resolve the card model once per page per frame.

**Card style** (`ClassPresentation.CardStyle`, `cardFor`):
The hover-dependent visual layer: border colour, hover wash and name colour. Derived per frame from `isHovering`.

**Card model** (`ClassPresentation.CardModel`, `cardFor`):
The whole card in one decision: text + style together, so the look is decided once instead of in ternaries across callers.
_Avoid_: card DTO, view model

**Picker screen** (`client/ClassSelectScreen`):
The picker itself: dimmed backdrop, one large focused card per page with its full kit
printed on the card and no tooltips anywhere. Side previous/next controls, clickable
dots and keyboard paging (arrows/`A`/`D`) move between pages with wraparound; the
signal carries the current class so reopening lands on its page. One pinned `Select`
control per page, title, subtitle and the `Click a class to choose it` footer. It positions
widgets from the renderer's focused math, resolves one `CardModel`
per frame; how the card draws is the card renderer's business, and what the card says is the card text's. It never pauses and never closes on Esc. A new class definition adds its own page with no layout change.
_Avoid_: GUI, menu

**Card layout** (`client/CardLayout`):
Everything the focused card sits in: the focused math (centered card rectangle,
two-thirds width up to 520, pinned select rect, side arrows, dot row,
title/subtitle/footer anchors), the header's vertical model (name, tagline and divider
anchors with the derived body top, the divider height, the inner width), the hover
hit-test the screen positions with, and page wraparound. Rows past the visible slice
scroll under the wheel with a thin scrollbar; the pinned select never moves, and a new
page always opens at the top.

**Card renderer** (`client/ClassCardRenderer`):
Everything the focused card looks like: body, accent strip, border and hover highlight,
plus the name, tagline, divider and wrapped detail lines. The screen measures the body
once per frame (`measure` → `Measured(rows, layout)`), clamps the same frame's scroll
from the result, then draws from it. The wrap and the measure take the width as a function of
the string — the screen passes `font::width` — so both are assertable with a stub measure and
neither reads a `Font`. The first wrapped row
of a line draws in its own color, continuations draw dimmed so names scan, and the tone
boundary is the line's own derived `descStart()`. The frame and the body both draw from
one card model; the screen passes the model,
never the parts. Each visual line draws with
the colour and weight it carries, so nothing here reads what a line says. Long lines
wrap inside the card and scroll above the pinned select.
_Avoid_: card widget, draw helper

### Status HUD

**Status HUD** (`StatusHud`, `StatusHudElement`):
The one thing on screen that shows class state: a vertical text stack anchored to the left
screen edge, the block vertically centred. One row per chip family — the Legionary's stance,
the signature lane, the ultimate lane, then the state rows (Momentum with its checkpoint
word, Streak) — with the anchored
chip's name first and its number after it, and riders after the number as dotted counts. The
names are the glossary's own nouns in Title Case, never keys. Countdowns are whole seconds,
rounded up, so a live chip never reads zero; a ready lane draws dim with no number, and the
missing number is what marks it ready. A single space holds a word and its number together —
rows read as written, numbers ragged down the stack, never padded into a column. The rows
come from the pure `StatusHud.rows` decision; the element only draws them and owns hiding
(classless, F1, any open screen, no snapshot). No icons and no textures ship, no config file
exists and there is no layout knob: the left-centre anchor is a constant.
_Avoid_: strip, overlay, widget

**Rider**:
A chip that draws on its lane's row after the lane's number: a window or one-shot that rides
its ability rather than owning a row. Riders are bare dotted counts — `Offload 10s · 5s`; one
window per lane, so the number needs no word — with one exception: the Roulette outcome keeps
its band word (`· Jackpot 8s`) because which band spun is the information. There are no
badges and no `+` marker: the `IMP`/`TES`/`AEQ`/`ALL` vocabulary retired with ADR-0021, and
the Offload window's count reads the same spent or loaded. A one-shot's rider vanishes on the
spend (the Rotate Impetus rider works that way); a Zero outcome draws no rider at all.
_Avoid_: window chip, badge

**Status snapshot** (`PlayerStatusPayload`):
The S2C `infusedclasses:player_status` snapshot and wire payload in one: the class id, a
`LaneRead` per lane, Momentum stacks plus grace and the grace stamp, the stance, the flip
target, Streak and the Roulette outcome. Built from the resolved class
definition, the tick's one transient state read taken at a `now`, and the runtime the
stack facts come from, so it can be asserted without a player — the
builder never touches the registry, and a missing class is the empty shape. Its codec and its
`movedSince` loop the lanes, so the wire order cannot drift from the read and a seventh window
adds no field. `movedSince` is the emission
guard: the discrete fields and each lane's read only — including the grace stamp, which moves on
every re-stamp, so a
hit at the stack cap still reaches the client — never the raw remainders that decay every
rebuild. An empty class id is a state, not an absence — a classless join sends one so the
client hides the stack by fact.
_Avoid_: state packet, sync payload

**Status sync** (`StatusSync`):
The emission adapter: one last-sent snapshot per player UUID. `maybeSend` runs in the server
tick, the one steady-state change point (ADR-0022) — the tick hands it the class, the runtime
and the one state read it already resolved — and sends only when `movedSince` says something
moved, so a rejected
press, a spam swing or a quiet tick costs no packets and everything else reaches the client
on the next tick. `sendNow` forces one snapshot on join and respawn (classless included, no
runtime materialised); `forget` drops the entry on logout.
_Avoid_: broadcaster, syncer

**Status store** (`StatusStore`):
The client's one snapshot copy plus its receipt time. The HUD element interpolates its
countdowns from that stamp and reads no other status state; a disconnect clears both fields,
because the snapshot is session data.
_Avoid_: cache

### Protocol and commands

**Select intent** (`SelectClassPayload`):
The client's `infusedclasses:select_class` intent: "I picked this class". The server validates it
against the choice module; a rejected pick leaves the picker open for retry. An accepted switch
clears what the old class earned and resets the cooldown, so no stale modifier survives.
_Avoid_: select packet

**Signature intent** (`RushPayload`):
The client's `infusedclasses:rush` intent, the signature slot's: "I pressed the
signature key", sent while the `R` key (`key.infusedclasses.signature`) is
consumed each client tick. The payload class and channel keep their old `rush`
names on purpose — the wire is not the player surface, so the protocol and the
player vocabulary diverge by design (ADR-0027). The server validates class and cooldown;
rejected presses leave the lane and the window untouched.
For a Juggernaut the press fires Offload; for a Legionary the same bind arrives as the rotate
intent and for a Gambler as the Roulette spin, so binds survive the class.
_Avoid_: shout packet, key packet, Rush intent

**Ultimate intent** (`RendPayload`):
The client's `infusedclasses:rend` intent, the ultimate slot's: "I pressed the
ultimate key", sent while the `V` key (`key.infusedclasses.ultimate`) is
consumed each client tick. The payload class and channel keep their old `rend`
names on purpose — the wire is not the player surface, so the protocol and the
player vocabulary diverge by design (ADR-0027). The server validates class, stacks and
cooldown; rejected presses leave the lane and the window untouched.
For a Juggernaut the press fires Onslaught; for a Legionary the same bind arrives as the oath
intent (Aequilibrium in any stance) and for a Gambler as All In, so binds survive the class.
_Avoid_: heal packet, key packet, Rend intent

**Picker signal** (`OpenClassScreenPayload`):
The server's `infusedclasses:open_class_screen` signal telling the client to open the picker,
carrying the player's current class id (empty when none) so reopening lands on its page. Sent
once, on first join when the player has no class, and on demand by the select command.
_Avoid_: open packet

**Pick confirm** (`ClassConfirmedPayload`):
The server's `infusedclasses:class_confirmed` reply. The client closes the picker only on this, so
a rejected pick stays open for retry.
_Avoid_: confirm packet

**Class command** (`ClassCommands`):
The `/class` tree. Bare `/class` and `/class info` are open to everyone and read for the
invoking player: the class display line, one line per carried lane with its `%.1fs`
remaining, the Legionary's stance, and `No class chosen` when classless, composed by the
pure `ClassInfo.lines` decision. `/class set <player> <class>` suggests registered ids
(playable only), parses a namespaced id through the vanilla identifier argument, and
validates in the choice module (`Unknown or locked class` on failure); an accepted
switch clears what the old class earned like the picker path does; `/class pick` (gamemaster-
gated per player) reopens the invoking player's picker on their current page with no chat
output, and `/class select <player>` is testing-only, gamemaster-gated, reopening the picker
on the target's current page for GUI iteration.
_Avoid_: admin command, cheat

## Decisions

Recorded in `docs/adr/`. `0001` is the decide-then-apply rule these terms are named after; `0002`
owns the choice/transient split; `0003` defers the class-lookup seam until a second source of
classes exists; `0004` inverts the Juggernaut kit to high-HP rewards (Vigor plus Momentum, the Rush engage
plus the Rend window) with sound replacing root — superseded by `0027`; `0005` moves the hit-gating decision out of the
server entry, the stack storage out of the transient runtime, and the trigger scan onto the class;
`0006` adds the Legionary stance tradeoff plus the universal charged curve and stack gate (the
curve point revised by `0028`);
`0007` deepens the melee hook (plan, settle, morph) and splits the adapters, revisiting
the `0005` bind-morph carve-out; `0008` closes the review's six seams (cooldown leak,
one-line Rend adapter, shared gate-and-arm, full hit plan, card back door, window registry);
`0009` keeps oath dispatch in the server entry (morph stays pure) and single-sources
the Legionary marker through `hasAbility`; `0010` pays the flip instead of the camp
(halved base auras, Impetus/Testudo as switch buffs off the flip record) and gives `G`
one clean ultimate (Aequilibrium on the reused oath lane, morph deleted);
`0011` folds the stance adapter back into the passive applier (one adapter was
a hypothetical seam, reopening `0007`) and homes both flip buffs on Rotate with
one Legionary marker; `0012` retires vanilla crits for every player
(`canCriticalAttack` cancelled), replaces them with the universal 10% ×1.25
full-swing crit for classless and classed alike, and makes chance a passed-in
value: the entry draws the roll, `CritRoll` decides it, a class's `CritRule`
gives it dice. `0013` deepens the review's remaining seams: actives carry their
own ability ids (the parallel id list is gone), one `AbilityOutcome` envelope replaces
six per-ability result protocols, `ClassSwitch` is the one switch path, ability windows
carry their duration (reads take only the clock), `MeleePlan.resolve` plans and settles
in one call (reopening `0007`'s no-JUnit cost for the settle), card geometry lives in
`CardLayout` anchors with one measure per frame, and the card tone boundary derives
from the join. `0014` reinstates the HUD as the slot strip (`.scratch/passive-hud` is
closed with the effect-row approach): the rows decision stays pure
(`StatusHud.rows`), one client element at its own `infusedclasses:status` id draws it,
the server snapshots at change points into one payload guarded by `movedSince`, and
chat joins the strip's one-decimal countdowns. `0017` retunes the strip to
self-contained pills whose words left with their labels, and lets a press on
cooldown fall silent — one clock, on screen, not two. `0018` settles the
argument by cutting the boxes entire. 0018 passed through two looks: first a
bare sweep line along the hotbar's side, and when the hotbar margin clipped a
full roster in-game, the sweep gave way to a flush screen zone — one line per
row, flush 2 gui from the screen's edge, vertically centered — with
`hud.scale` owning the size and the old hotbar spellings carrying their side
in the config. `0017`'s silence
and compressed numbers carry into the stack. `0019` scraps this chain from the
build pending a later reimplementation: draw layer, client config file, the
snapshot payload and its sync callers all leave; the pill-look lessons live in
the prototypes. `0020` brings the HUD back as the left-centre icon stack: the
rows decision and the transport return from the archive, anchored chips trade
the old badge names for generated 16x16 icons while riders keep the badge
vocabulary, countdowns print whole seconds rounded up, and no config file
exists. `0021` makes the stack speak words: the icon column gives way to a text
stack — glossary names with a single space before their numbers (no padded
column; the first cut read as tab stops), riders as bare dotted numbers
except the Roulette band word, the Rush `+` marker dropped, the ten generated
icons, their script and the whole texture tree deleted — and the snapshot
carries the Momentum grace stamp, so a re-stamp at the stack cap is never
invisible to the client. The same decision renames the code's window nouns:
`stride` and `empower` leave identifiers, comments, tests and the picker copy
for `SPEED_BONUS` at `infusedclasses:rush_speed`, `RendAbility.bonusFor` and the
`peekRendBonusStacks` / `consumeRendBonus` pair. `0022` makes the tick the one steady-state
emission point: the post-branch sends (both intent receivers, the hit settle, the accepted
switch) call `maybeSend` no more — a press, a spend, a decay or a switch reaches the client on
the next tick, at most 50 ms — while join and respawn keep their forced sends and a quiet tick
still sends nothing.
`0023` puts the stack's view on the roster: a row carries the payload lane read and its
rider rules, so the HUD's rows decision keeps only the class-shape pick and a new
ability adds a row instead of another branch. `0024` folds the cooldown reject into the
one silent shape: `AbilityOutcome` loses its machine-readable remainder and its
`onCooldown` factory, and the per-ability `Result` rows keep one `rejected()`.
`0025` makes the lane the key: `Lane` owns each cooldown length, `Cooldowns` is one
`use`/`remaining`/`reset` triad, and `PlayerRuntime` and the snapshot each carry one
`LaneRead` per lane, so a seventh window is one enum value plus its arm site (revising
`0023`'s `View` and `0021`'s Rend-pair names). `0026` stops lending the ultimate: each bind
target names the ability it fires, one walk of the class's own actives answers both the slot
and the row that draws it, so the rows decision's class-shape pick moves into the shape
(revising `0009`'s shared-marker keying and `0023`'s ultimate-bind sentence).
`0027` makes Momentum the Juggernaut's one mechanic: a 60-stack bar fed +1 by both dealt and taken
full hits, bleeding 1/s after 5s quiet, reading checkpoints at 15/30/45 that each add armor,
weight and knockback resistance; Rush and Rend retire for Offload (a flat 5-stack price for a 5s
haste/speed window) and Onslaught (a 10-stack gate, 15s or until the bar empties, +1 per full hit
at 2 stacks each, no gain while live); Vigor is cut; Burden (+1 damage, -0.5 attack speed) is the
always-on chassis; and the charged bonus curve stays, pricing flat ability damage with the
full-swing gate still blocking spam (the Onslaught pay shape, Burden's damage row and the
movement weights revised by `0028`). `0028` makes one unit per stat family: damage power becomes a
deal mult (Burden, Gladius, Aequilibrium and Onslaught all x1.15 at the iron-sword reference,
composed in the plan's combined), attribute stats stay flat in their own unit (movement rows on
one 0.005 grid, copy in the same unit), chances stay percents, the charged bonus curve retires —
`FullSwing.isFull` keeps the shared gate — and balance gains a target (±10% expected payout at
two references) and a sheet (`.scratch/balance-pass/`, revising `0006`'s curve and `0027`'s
Onslaught price and movement weights). The sheet's tuning round (defense measured per hit on
the same timeline as offense, band checked around the mean) settled Burden's swing at -0.4,
Gladius's glass tax at x1.2, the dice at 15% base and All In at x1.75; Scutum stays as it
reads. The wire
names `rush`/`rend` stay on the reused signature and ultimate intents.
