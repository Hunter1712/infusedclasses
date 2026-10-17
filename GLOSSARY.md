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
The decision of which attribute modifier a conditional passive's current stacks are worth, as an
attribute, a modifier id and an amount. Zero or fewer stacks mean the passive contributes nothing, and
that answer is an absent modifier rather than a zero-valued one.
_Avoid_: passive bonus, armor bonus, stack effect (the decision is not the thing on the player)

**Passive applier** (`PassiveApplier`):
The adapter that makes the player's attributes agree with the passive effect. It writes only when
the applied value differs from the wanted one, following the decision's plan: write the wanted
modifier, take off what dead stacks earned, or nothing. A tick that moved no stacks cannot have
changed what should be on the player, so it needs no attribute write. Movement travels per
passive (`tickDecayMoved`), so a decayed passive never removes one sitting at zero; a class
switch removes what the old class earned but the new one does not carry (`onClassChanged`),
drops the old stacks and resets the cooldown. The hit settle now belongs to the melee
plan (see `Melee plan`); the spender half stays here: `consumeSpent` resolves the priced
passive and drops its stacks, `dropStacks` is its JUnit-reachable core, and the
attribute removal rides with the player.
_Avoid_: passive system, stat updater

**Ability outcome** (`AbilityOutcome`):
The shared envelope every ability decision returns: whether the press fired, the
announcement that goes with it, and the machine-readable remainder when a lane was on
cooldown. `fired`, `rejected` (silent), `fizzled` (says its line, no cast) and
`onCooldown` are the four factories. An ability with a payload wraps the envelope in its
own `Result` record instead of widening it: Rotate's landed stance, Roulette's band,
Rend's consume flag. The empower amount is not part of any of them: the fire stamps the
stack count on the runtime, and hit time recomputes the amount from it.
`trigger` gates read-only; `fire` gates and arms the transient runtime's cooldown together, so no
caller can fire without arming, and rejected presses leave the cooldown untouched.
_Avoid_: applied outcome, ability result (both name the wrapper), buff

**Ability applier** (`AbilityApplier`):
The shared adapter seams every ability outcome and class-choice outcome passes through: the announcement
on the action bar (abilities, rotate) or chat (class-choice), plus the cast fanfare (`playCast`) for fired abilities. Effect and sound ids resolve
at their single use sites — Rush's stride stat in its own adapter, the cast sound in the
fanfare itself — never behind shared lookups with one caller each. A result with nothing to grant and nothing
to say — a wrong-class, on-cooldown or no-stacks press — lands nothing.
A fired-or-fizzle press lands through `announceAndCast` alone — Rotate with no
sound (it carries no cast sound, so it announces without fanfare); only Rush keeps its own
adapter — for the cleanse and the stride it lays — routing its fanfare
through the shared path. Class-choice rejections and accepts route through `announce` with the `actionBar` flag.
_Avoid_: ability system, effect handler

**Gate-and-arm** (`AbilityGate`):
The one rule every ability firing shares: a rejected trigger passes through untouched,
a fired trigger arms its lane, and an arm that somehow fails reports on-cooldown instead
of a fire it did not earn. Abilities keep their own numbers, chat lines and windows;
this owns only the fallback.
_Avoid_: fire helper, trigger wrapper

**Chat line** (`ChatLine`):
The text and the chat style a decision returns for the server entry to send verbatim.
Rend's fire line is `Rend!` in red, Rush's is `Rush!` in yellow. Fire lines and fizzles
speak only what happened: a press on cooldown says nothing — the Status HUD lane already
counts that number live (ADR-0017) — so `ChatLine.cooldown` (`%.1fs`, one decimal) is
left as the `/class info` lane shape. Its wording, rounding and locale are decided with the ability (or class-choice outcome),
not formatted by whoever sends it. Class-choice lines use the same seam: `unknownOrLocked()` (red, action bar)
for a rejected pick, and `classPicked(displayName)` (gray, chat) for an accepted switch.
_Avoid_: message, notification, cooldown echo

**Picker copy** (`ConditionalPassive.fullDesc`):
The decision of what a conditional passive's picker copy says, generated from its own numbers so it
cannot drift from behaviour. Every detail line shows name plus short copy
(`Land a hit: +0.4 armor (max 20). 5s no hit: lose all.`).
Copy stays short on purpose: every active desc holds at most 200 chars, pinned by
test, so full kits fit the focused card with little scroll.
The two triggers the mod emits are described as events ("Land a hit"
for `on_hit_dealt`, "Take a hit" for `on_hit_taken`);
any other trigger reads as the bare `Trigger`. Whole numbers read
as `1`, not `1.0`.
_Avoid_: description, lore text

### Classes, passives and stacks

**Class** (`ClassDefinition`):
A playable archetype, registered once with its id, display name, passives, accent
colour, tagline, the actives list (one row per fired active: its ability id, name and
picker description) plus the flip-buff list (one `FlipBuff` row per trigger-fired
active: name plus description, shown under `Passives`). A class is data: it carries no player and no state of its own. The first passive on a
trigger is found with `passiveForTrigger`, never by list position. Juggernaut, Legionary and Gambler are the three
playable classes today: `Juggernaut`, Rush (`infusedclasses:rush`) plus Rend
(`infusedclasses:rend`), accent `0xFFE74C3C`,
tagline `Frontline bruiser`, actives `Rush` and `Rend`, with picker copy
generated from the ability constants, so the numbers
cannot drift from behaviour; `Legionary` (see below), Rotate plus the Impetus/Testudo
flip buffs and the Aequilibrium oath ultimate, accent `0xFFB87333`, tagline `Sword and shield`;
`Gambler` (see below), Roulette plus All In and a crit rule that doubles as its
passives, accent `0xFFD4AF37`, tagline `Double or nothing`.
Juggernaut carries no max-health bonus (vanilla 20 HP):
Vigor below is the reward for staying topped, and Momentum is the armor for staying on target.
_Avoid_: character class definition, build

**Locked class**:
No locked classes exist: the definition carries no locked flag, the picker only
ever shows playable classes, and nothing rejects by lock. The word survives only
in the rejection line (`Unknown or locked class`).
_Avoid_: unavailable class, future class, hidden class

**Active** (`ClassDefinition.ActiveAbility`):
One playable active on a class card: its ability id, name and picker description in one row.
The id is what `hasAbility` reads, so a card row and its gate cannot drift apart. A class
carries one per active — Juggernaut carries Rush and Rend — so names never join with `+`.
_Avoid_: ability (ambiguous with the gating id), spell

**Flip buff** (`ClassDefinition.FlipBuff`):
One trigger-fired Legionary buff on a class card: name plus picker description, shown under
`Passives` because no key presses it. Never gated: it carries no ability id and nothing reads
`hasAbility` for it.
_Avoid_: passive (it is not stack-driven), toggle

**Conditional passive** (`ConditionalPassive`):
A passive that earns stacks when a named trigger fires, rather than always being on. It carries its
own numbers — stack cap, attribute, per-stack value, grace period — and its picker copy
is generated from those numbers so the copy cannot claim something the behaviour does not do.
Momentum, the Juggernaut passive, is `on_hit_dealt`, max 20, `minecraft:armor` under
`infusedclasses:momentum_armor`, +0.4 per stack (max +8.0), 5s grace, all stacks drop at once after.
_Avoid_: buff, stackable stat

**Vigor** (`HpScaledPassive`):
The Juggernaut's HP-scaled passive pair: current HP maps to stepped buckets, 10% per step
over 10 steps, each step adding a fixed bonus on top of a floor — +0.3 attack damage
from -1.0 (`infusedclasses:vigor_damage`, -1 at 0 HP, +2.0 full) and +0.075 attack speed
from -0.25 (`infusedclasses:vigor_speed`, -0.25 at 0 HP, +0.5 full).
The pair reads as one `Vigor` line on the card (see `HpScaledPassive.group`),
with both bonus descs joined, so one passive never looks like two.
Recomputed every tick from live HP, transient and never saved; Rend spends
Momentum only and never touches it.
_Avoid_: rage, frenzy stacks

**Stack**:
One earned increment of a conditional passive, counted per player and capped at that passive's
maximum. Stacks are keyed by the passive definition, never by its name, so a passive is addressed
by what it is rather than by what it is called. Past the grace period every stack drops
at once, vanilla-style, and stacks are transient: they do not survive relog or respawn.
_Avoid_: charge, level, rank

**Cooldown** (`Cooldowns`):
The wait after an ability fires, tracked per player, also transient. Six lanes:
Rush's 25s (`RUSH_MS`) and Rend's 60s (`REND_MS`): `useRush` /
`useRend` fire only when their own lane is ready and report it, `rushRemaining` /
`rendRemaining` report the milliseconds left, 0 when ready; Rotate's 8s
(`ROTATE_MS`, `useRotate` / `rotateRemaining`, the only gate on both switch buffs);
the Legionary shared oath 50s
(`OATH_MS`, `useOath` / `oathRemaining`, owned solely by Aequilibrium); and the
Gambler's Roulette 25s (`ROULETTE_MS`, `useRoulette` / `rouletteRemaining`) and
All In 75s (`ALL_IN_MS`, `useAllIn` / `allInRemaining`) lanes.
`reset` readies every lane again
for class switch and respawn. The lanes ride one map behind a private lane enum,
so a fifth lane adds a value rather than a field; callers and tests cross the
per-lane verbs, never the map. The engage window rides on the transient runtime next to
the lanes: a fired Rush stamps the press time plus a one-shot next-hit bonus,
`hasRushBonusPending` reads it against the 5s window, and switch and respawn clear it with
everything else. Rush and Rend windows are one-shot (the melee plan peeks them,
its resolve spends them, each bonus landing at most once); Testudo and Aequilibrium
windows are timed (peeked while live, never consumed).
_Avoid_: timer

**Rush** (`RushAbility`):
The Juggernaut's engage on `R`: cleanses every active harmful effect, quickens
stride (+0.02 move speed, `infusedclasses:rush_stride`) for
5s (`WINDOW_TICKS`), and arms a one-shot bonus so the next full-swing melee hit inside
the window earns +5 Momentum (`BONUS_STACKS`), on its own 25s lane. Spam leaves the bonus pending. Fires with zero stacks at full
HP — engaging topped-off is the normal case, not a reject — and rejected presses leave
the lane, the window and the bonus untouched. Decided in the trigger/fire gate-and-arm
shape. A stat, never a vanilla effect: no effect icon, no effect dependency.
Like every cast, firing plays its sound (see `Spellcast`): a wind-burst whoosh for the engage.
_Avoid_: dash, charge

**Spellcast** (`AbilityApplier.playCast`):
The fanfare shared by every active: firing any ability plays its own sound — wind burst for
Rush, anvil land for Rend — so landing a cast is one learnable rule; fizzles and rejects cost
nothing and stay silent. The single play path lives here so appliers never duplicate it.
_Avoid_: cast time, wind-up

**Rend** (`RendAbility`):
The Juggernaut's Momentum spender on `V`: empowers the next melee hit landed within 6s
(`WINDOW_TICKS`) for `0.5 HP per stack` bonus damage (10 HP at the full 20), and consumes
every Momentum stack plus the armor they granted up front, on its own 60s lane. An
unswung window simply expires with the stacks already spent. Zero stacks fizzles with a
`No Momentum` line and arms nothing. Decided in the trigger/fire
gate-and-arm shape; the spent passive is found once with `passiveForTrigger`
(never by position), the empower snapshot rides the transient runtime next to the lane, and
the consume flag rides in the wrapper result: the entry asks the passive applier to consume
that passive while `MeleePlan.resolve` spends the snapshot on the next hit and returns the
stamped bonus in its `Landed`, landing only what it settled, and only when the plan saw one
live, so empty hits never touch a window.
Like every cast, firing plays its sound (see `Spellcast`): an anvil-land smash for the
empower.
_Avoid_: heal button, second ability

**Legionary** (`ClassRegistry`):
The second playable class: `infusedclasses:legionary`, display `Legionary`,
accent bronze `0xFFB87333`, tagline `Sword and shield`, actives Rotate plus the
Impetus/Testudo flip buffs and the Aequilibrium oath ultimate. It carries no conditional passives and
no HP-scaled pair: its stances are transient auras on the runtime (see below),
not stacks, so the card groups the flip buffs under `Passives` and the play lives
in the fired actives. Every Legionary enters in `Scutum`.
_Avoid_: roman, tank class

**Gladius**:
The Legionary's striking stance: `attack_damage` +1.0
(`infusedclasses:gladius_damage`), `attack_speed` +0.3
(`infusedclasses:gladius_alacrity`), melee taken x1.1. Glass reads as glass.
_Avoid_: dps stance, sword form

**Scutum**:
The Legionary's plate stance and the default: `armor` +3
(`infusedclasses:scutum_plate`), `armor_toughness` +1
(`infusedclasses:scutum_toughness`), `movement_speed` about -6%
(`infusedclasses:scutum_weight`), melee dealt x0.9. Plate reads as plate.
_Avoid_: tank stance, shield form

**Rotate** (`RotateAbility`):
The Legionary's stance toggle on `R`: flips `Gladius`/`Scutum` any time off its
8s lock (`ROTATE_MS`), no build-up, no stacks. Picking a stance applies its aura
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
Flipping away drops it with the stance that earned it. No lane, no window to arm.
_Avoid_: enrage, burst

**Testudo** (`RotateAbility`):
The Legionary's Scutum-flip buff: melee taken pays x0.85 (`SWITCH_TAKE_MULT`)
for 1.5s from the flip (`SWITCH_TAKE_MS`). Timed, never consumed: every hit
inside the catch is discounted. No lane, no window to arm.
_Avoid_: block, turtle

**Aequilibrium** (`AequilibriumAbility`):
The Legionary's clean oath ultimate on `V`: +1.0 attack damage
(`infusedclasses:aequilibrium_edge`) + +3.0 armor
(`infusedclasses:aequilibrium_plate`) for 6s (`WINDOW_TICKS`), in either stance,
on the shared 50s oath lane (`OATH_MS`, `useOath` / `oathRemaining`). No
deal/take mults, no knockback immunity, no downsides: the stance stays
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
The Gambler's ultimate on `V`, 75s lane: crits deal ×2.0 instead of ×1.5 for
15s. The dice rules never bend: betrayal, Streak resets and every band stay as
they are.
_Avoid_: double damage, ultimate form

**Swing charge** (`SwingChargeAccess`):
The charge vanilla read for the swing being settled, captured one line before
vanilla zeroes the attack-strength ticker in `onAttack`. The damage hook runs
after that reset, so a read from there is always 0 and every full-swing gate
(dice roll, stack bump, Rush credit, the Rend curve) would read zero; the capture
is the hook's only source for it.
_Avoid_: attack cooldown, swing timer

**Bonus curve** (`BonusCurve`):
The universal melee rule: mod hook bonus damage scales quadratically with swing
charge (`bonus x charge x charge`, half charge pays a quarter, spam pays chips).
Base weapon damage never routes here — vanilla already halves spam. One call
site in the attacker branch; every melee class shares it, no per-class copies.
`isFullSwing` is the one definition of a full swing both the curve and the gate call.
_Avoid_: damage falloff, charge multiplier

**Melee plan** (`MeleePlan`):
The decision of what one melee hit earns and settles, taken from definitions and
transient state: whether the swing earns stacks (full swings only, spam earns zero),
the stance deal and take mults (1.0 for anyone else), the wheel's live deal mult and
the Impetus flip buff (pending only, spent once, any charge). `planHit` is the pure half:
it decides the whole hit in field order — bump, Rush credit, Rend spend, Impetus spend,
dice verdict, combined mult (deal x take x dice) — peeking only. `resolve` plans through
it and settles the runtime movements in the same order at the same moment, returning
`Landed(rendHp, verdict, combined)` for the entry to land, so the pairing the plan saw
is the pairing that lands and empty hits never touch a window; the attacker branch
(`settleAttackerHit`) lands exactly what the resolve returned (cue, Rend bonus, combined
mult) so the hook cannot reorder or half-apply it; Gladius plus Impetus reads
x1.1 deal, Scutum plus Testudo x0.85 take, a crit x1.25 deal, named once in the plan rather than emerging in the
entry. The server entry maps, adapters
apply; the victim runtime arrives lazily, so a non-Legionary victim costs no transient
state. Owns every ability-id check the hook needs, so the entry never compares
ability ids to gate rewards.
_Avoid_: damage handler, hook logic

**Damage settle** (`DamageSettle`):
The single guarded hurt for the whole mod plus the stance-mult settle every melee
branch funnels through. Over 1.0 lands extra hurt (dead targets deal nothing); under
1.0 heals back the discount, so the net hit reads as base times mult; exactly 1.0
touches nothing. Rend and Impetus bonuses land here too. Vanilla drops any same-tick
follow-up that does not beat `lastHurt`, so the extra goes in with that memory
cleared and leaves it at base plus bonus: the step lands silently with armor, death
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
write-only-on-change shape as the Vigor tick. Aequilibrium edge and plate ride the window
while live. Both sides iterate the stance's single aura list (`RotateAbility.auraFor`,
`AequilibriumAbility.windowModifiers`), so write and remove cannot drift. The Roulette
outcome window's stat auras ride the same tick (`applyRoulette`) and come off when the
Gambler leaves (`removeRouletteModifiers`). Leaving the class
takes them all off through `onClassChanged`, which also owns the Legionary marker.
_Avoid_: stance system, aura updater

**Bind morph** (`BindMorph`):
The decision of what a slot bind means for a class: the signature slot (`R`)
arrives as the rotate intent for Legionaries, the roulette intent for Gamblers,
and Rush for everyone else; the ultimate slot (`V`) arrives as the
oath ultimate for Legionaries in any stance, All In for Gamblers, and Rend for
everyone else. One ultimate per class.
The intent receivers wire, this decides.
_Avoid_: key routing, bind table

**Stack gate**:
The other half of the spam fix: the attacker branch reads `attackStrengthScale`
once per swing and feeds conditional triggers only at full charge
(`MeleePlan.shouldBump` over `BonusCurve.isFullSwing`), so spam
earns zero stacks and timed swings earn one. Rush bonus is gated on the same bump
and scales via the curve. Crit rolls and Streak movement ride the same gate:
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
The Gambler's crit rule: 20% crit at ×1.5, 5% betrayal at ×0.5, +15
percentage points of crit per non-crit full hit. The crit band locks at 95%
from streak 5; betrayal stays 5% forever.
_Avoid_: luck passive

**Betrayal**:
The fixed band at the foot of the Gambler's crit roll: 5% of full-charge hits
deal ×0.5. It never grows and is never removable, and it still feeds Streak.
_Avoid_: backlash, mishap

**Streak**:
The Gambler's loaded pocket: +15 percentage points of crit chance per non-crit
full hit (a betrayal counts as one); any crit resets it to base. The crit band
stops growing at 95% from streak 5. A transient counter like every earned
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
clean what the old class earned (`PassiveApplier.onClassChanged`) and announce the outcome,
rejection included; it returns the new definition, or null when the pick was rejected.
Callers keep only their own extra — the picker's confirm packet, the command's return
code — so the two entries cannot sequence the switch differently.
_Avoid_: class change, switch handler

**Transient runtime** (`PlayerRuntime`):
The per-player stacks, cooldowns, ability windows and the Gambler's Streak
counter that last only as long as the session. It is handed the class they
belong to and the current time on each call, so it holds neither a clock nor a reference to anyone's
choice, and it is owned per player by the server entry rather than stored on the player. Stack
storage (counts plus last-trigger timestamps in definition-keyed maps) lives in `PassiveStacks`:
`stacks` reads one passive, `onTrigger` bumps
every passive on the class whose trigger matches (unknown triggers, a missing class and trigger-less
classes are inert, each bump re-stamps grace), `tickDecayMoved` drops every stack of each
passive on the class past its own grace, and reports the set that moved. `clear` drops one class's
stacks; cooldowns go through the lane verbs, armed only by ability `fire` and reset by the adapter
on switch. Ability windows carry their own duration, stamped at arm time: reads take only the
clock, and each ability names its window constant once, at its arm site. Callers and tests
cross this module's seam; the stacks module is its implementation.
_Avoid_: player state, live state, runtime (too generic on its own)

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
from the result, then draws from it. The first wrapped row
of a line draws in its own color, continuations draw dimmed so names scan, and the tone
boundary is the line's own derived `descStart()`. The frame and the body both draw from
one card model; the screen passes the model,
never the parts. Each visual line draws with
the colour and weight it carries, so nothing here reads what a line says. Long lines
wrap inside the card and scroll above the pinned select.
_Avoid_: card widget, draw helper

**Status HUD** (`StatusHud`, `client/StatusHudElement`):
The strip on screen for a class's live state: a stack of bare text zoned into the
screen — one line per row, no box, no icon, no texture, no dot — flush 2 gui from
the screen's chosen edge (left by default), the block vertically centered on the
screen, top-down with the signature row leading. A line is its facts only: a
cooling lane `SIG 25s`, a ready lane its dim bare badge, windows and stacks riding
their lane's line (`ALL 13s`, `MOM 2 4s`, a rider's bare `9.8s`), never a whole word — names stay
in fire lines and `/class info`. `rows(status, elapsed)` is the pure
half: given the last snapshot and the millis since it arrived, it returns the whole strip as
rows — the roster per class (badges `SIG`/`ULT`, `MOM`, `STR`, `GLA`/`SCU`, `IMP`/`TES`,
`AEQ`, `ALL`, and the Roulette outcome initials, never Zero), riders riding
their lane chips, the `+` marker only where the chip outlives its load, ready lanes dim with
no number, live chips lit, whole-second countdowns at ten and above and one decimal below,
clamped at zero — and the element draws
exactly those rows, one line each (ADR-0018: the strip goes bare in a screen zone —
it supersedes ADR-0014's side-label boxes, ADR-0017's pills and 0018's own
hotbar sweep in look, keeps every decision's content).
Hiding is the adapter's, never the decision's: no class, F1, any open
screen; `hud.enabled=false` stops the drawing and nothing else, `hud.corner` chooses the
screen zone (`left`/`right` — the retired hotbar spellings carry their side), `hud.scale` sizes the stack.
A zone line grows into the open screen, so no roster can clip it.
Badges are slot and state names, never keys: the strip never reads a KeyMapping, so a
remapped bind cannot make it lie (ADR-0014).
_Avoid_: cooldown bar, effect strip, buff icons, pills, side labels

### Protocol and commands

**Select intent** (`SelectClassPayload`):
The client's `infusedclasses:select_class` intent: "I picked this class". The server validates it
against the choice module; a rejected pick leaves the picker open for retry. An accepted switch
clears what the old class earned and resets the cooldown, so no stale modifier survives.
_Avoid_: select packet

**Rush intent** (`RushPayload`):
The client's `infusedclasses:rush` intent, the signature slot's: "I pressed the
signature key", sent while the `R` key (`key.infusedclasses.signature`) is
consumed each client tick. The payload class and channel keep their old `rush`
names on purpose — the wire is not the player surface, so the protocol and the
player vocabulary diverge by design. The server validates class and cooldown;
rejected presses leave the lane, the window and the bonus untouched.
For a Legionary the same bind arrives as the rotate intent and for a Gambler as
the Roulette spin, so binds survive the class.
_Avoid_: shout packet, key packet

**Rend intent** (`RendPayload`):
The client's `infusedclasses:rend` intent, the ultimate slot's: "I pressed the
ultimate key", sent while the `V` key (`key.infusedclasses.ultimate`) is
consumed each client tick. The payload class and channel keep their old `rend`
names on purpose — the wire is not the player surface, so the protocol and the
player vocabulary diverge by design. The server validates class, stacks and
cooldown; rejected presses leave the cooldown untouched, and the empower lands
on the next melee hit inside the window.
For a Legionary the same bind arrives as the oath intent (Aequilibrium in any stance) and for a
Gambler as All In, so binds survive the class.
_Avoid_: heal packet, key packet

**Picker signal** (`OpenClassScreenPayload`):
The server's `infusedclasses:open_class_screen` signal telling the client to open the picker,
carrying the player's current class id (empty when none) so reopening lands on its page. Sent
once, on first join when the player has no class, and on demand by the select command.
_Avoid_: open packet

**Pick confirm** (`ClassConfirmedPayload`):
The server's `infusedclasses:class_confirmed` reply. The client closes the picker only on this, so
a rejected pick stays open for retry.
_Avoid_: confirm packet

**Status snapshot** (`PlayerStatusPayload`, `client/StatusStore`):
The strip's state at send time, carried by the server's S2C `infusedclasses:player_status`
signal beside its payload siblings and read by the status HUD element alone: class id,
per-lane remaining ms, per-window remaining ms plus their load flags, Momentum stacks plus
grace, stance, flip target, Streak, Roulette outcome. Emission is guarded by `movedSince`:
the server sends one only when something a client cannot derive has changed — a fired
ability, a melee settle, a moving decay tick, a class switch — and never for pure time
decay; join and respawn send unconditionally so a client always learns its class
(`StatusSync` holds the last-sent snapshot per player UUID). The client holds one: the last
snapshot plus its receipt time, overwritten whole per payload, cleared on disconnect
— countdowns interpolate from the receipt time, and no second copy of status state exists
client-side (ADR-0014).
_Avoid_: status packet, status cache, HUD state

**Class command** (`ClassCommands`):
The `/class` tree. Bare `/class` and `/class info` are open to everyone and read for the
invoking player: the class display line, one line per carried lane with its `%.1fs`
remaining, the Legionary's stance, and `No class chosen` when classless, composed by the
pure `ClassInfo.lines` decision. `/class set <player> <class>` suggests registered ids
(playable only), parses
one word and validates in the choice module (`Unknown or locked class` on failure); an accepted
switch clears what the old class earned like the picker path does; `/class pick` (gamemaster-
gated per player) reopens the invoking player's picker on their current page with no chat
output, and `/class select <player>` is testing-only, gamemaster-gated, reopening the picker
on the target's current page for GUI iteration.
_Avoid_: admin command, cheat

## Decisions

Recorded in `docs/adr/`. `0001` is the decide-then-apply rule these terms are named after; `0002`
owns the choice/transient split; `0003` defers the class-lookup seam until a second source of
classes exists; `0004` inverts the Juggernaut kit to high-HP rewards (Vigor plus Momentum, Rush engage
plus Rend empower) with sound replacing root; `0005` moves the hit-gating decision out of the
server entry, the stack storage out of the transient runtime, and the trigger scan onto the class;
`0006` adds the Legionary stance tradeoff plus the universal charged curve and stack gate;
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
and compressed numbers carry into the stack.
