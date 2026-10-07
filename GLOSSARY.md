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
drops the old stacks and resets the cooldown.
_Avoid_: passive system, stat updater

**Applied outcome** (`RendAbility.Result`):
The description an ability decision returns of what should be applied: how much
to deal, whether to consume stacks, plus the announcement that goes with it.
`trigger` gates read-only; `fire` gates and arms the transient runtime's cooldown together, so no
caller can fire without arming, and rejected presses leave the cooldown untouched.
_Avoid_: ability result (that is the whole decision, gating included), buff

**Ability applier** (`AbilityApplier`):
The shared adapter seams every ability outcome passes through: effect-holder lookup,
the announcement on the action bar, and the cast fanfare (`playCast`). A result with nothing to grant and nothing
to say — a wrong-class, on-cooldown or no-stacks press — lands nothing.
_Avoid_: ability system, effect handler

**Chat line** (`ChatLine`):
The text and the chat style an ability decision returns for the server entry to send verbatim,
including the remaining-seconds message for a press on cooldown. Rend's fire line is
`Rend!` in red, Rush's is `Rush!` in yellow; a cooldown line is `Rend: %.0fs` with no style of its own,
rounded with `%.0f`. Its wording, rounding and locale are decided with the ability, not
formatted by whoever sends it.
_Avoid_: message, notification

**Picker copy** (`ConditionalPassive.fullDesc`):
The decision of what a conditional passive's picker copy says, generated from its own numbers so it
cannot drift from behaviour. The card body shows the passive name only; the tooltip uses the full
form. The two triggers the mod emits are described as events ("Taking a hit
grants 1 stack" for `on_hit_taken`, "Landing a hit grants 1 stack" for `on_hit_dealt`);
any other trigger reads as the bare trigger. Whole numbers read
as `1`, not `1.0`, and the drop reads as instant (`lose all stacks`).
_Avoid_: description, lore text

### Classes, passives and stacks

**Class** (`ClassDefinition`):
A playable archetype, registered once with its id, display name, locked flag, passives, active
ability ids, accent colour, tagline and the actives list (one name plus picker description per
active). A class is data: it carries no player and no state of its own. The first passive on a
trigger is found with `passiveForTrigger`, never by list position. Juggernaut is the one
playable class today: `Juggernaut`, Rush (`infusedclasses:rush`) plus Rend
(`infusedclasses:rend`), accent `0xFFE74C3C`,
tagline `Frontline bruiser`, actives `Rush` and `Rend`, with picker copy
generated from the ability constants, so the numbers
cannot drift from behaviour. Juggernaut carries no max-health bonus (vanilla 20 HP):
Vigor below is the reward for staying topped, and Momentum is the armor for staying on target.
_Avoid_: character class definition, build

**Locked class**:
A registered class that cannot be chosen yet. It carries no passives and no actives, and the registry
rejects a locked definition that does; a playable definition without an active is rejected the same
way. A locked class still carries picker text — accent, tagline — and is still shown in the picker.
Ranger and Mage are the two locked classes today.
_Avoid_: unavailable class, future class, hidden class

**Active** (`ClassDefinition.ActiveAbility`):
One playable active on a class card: its list line plus its tooltip description. A class carries
one per active — Juggernaut carries Rush and Rend — so names never join with `+`.
_Avoid_: ability (ambiguous with the gating id), spell

**Conditional passive** (`ConditionalPassive`):
A passive that earns stacks when a named trigger fires, rather than always being on. It carries its
own numbers — stack cap, attribute, per-stack value, grace period — and its picker copy
is generated from those numbers so the copy cannot claim something the behaviour does not do.
Momentum, the Juggernaut passive, is `on_hit_dealt`, max 10, `minecraft:armor` under
`infusedclasses:momentum_armor`, +1.0 per stack, 4s grace, all stacks drop at once after.
_Avoid_: buff, stackable stat

**Vigor** (`HpScaledPassive`):
The Juggernaut's HP-scaled passive pair: current HP maps to stepped buckets, 10% per step
over 10 steps, each step adding a fixed bonus — +0.3 attack damage
(`infusedclasses:vigor_damage`, max +3.0) and +0.05 attack speed
(`infusedclasses:vigor_speed`, max +0.5). Zero bonus at 0 HP, max at full HP.
The pair reads as one `Vigor` line on the card and tooltip (see `HpScaledPassive.group`),
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
The wait after an ability fires, tracked per player, also transient. Two independent lanes:
Rush's 25s (`RUSH_MS`) and Rend's 60s (`REND_MS`): `useRush` /
`useRend` fire only when their own lane is ready and report it, `rushRemaining` /
`rendRemaining` report the milliseconds left, 0 when ready, `reset` readies both again
for class switch and respawn. The engage window rides on the transient runtime next to
the lanes: a fired Rush stamps the press time plus a one-shot next-hit bonus,
`isRushLive` reads it against the 4s window, and switch and respawn clear it with
everything else.
_Avoid_: timer

**Rush** (`RushAbility`):
The Juggernaut's engage on `R`: cleanses every active harmful effect, lays Speed I for
4s (`SPEED_TICKS`), and arms a one-shot bonus so the next melee hit inside the window
earns +3 Momentum (`BONUS_STACKS`), on its own 25s lane. Fires with zero stacks at full
HP — engaging topped-off is the normal case, not a reject — and rejected presses leave
the lane, the window and the bonus untouched. Decided in the trigger/fire gate-and-arm
shape.
Like every cast, firing plays its sound (see `Spellcast`): horn for the engage.
_Avoid_: dash, charge

**Spellcast** (`Spellcast`):
The fanfare shared by every active: firing any ability plays its own sound — horn for
Rush, roar for Rend — so landing a cast is one learnable rule; fizzles and rejects cost
nothing and stay silent. The single play path lives here so appliers never duplicate it.
_Avoid_: cast time, wind-up

**Rend** (`RendAbility`):
The Juggernaut's Momentum spender on `G`: empowers the next melee hit landed within 6s
(`WINDOW_TICKS`) for `6 HP + 1 HP per stack` bonus damage, and consumes
every Momentum stack plus the armor they granted up front, on its own 60s lane. An
unswung window simply expires with the stacks already spent. Zero stacks fizzles with a
`No Momentum` line and arms nothing. Decided in the trigger/fire
gate-and-arm shape; the spent passive is found with `ClassDefinition.passiveForTrigger`
(aliased as `RendAbility.momentumOf`) rather than by position, the
empower snapshot rides the transient runtime next to the lane, and
the consume flag rides in the result: the entry asks the passive applier to consume
that passive while the damage hook spends the snapshot on the next hit.
Like every cast, firing plays its sound (see `Spellcast`): roar for the
empower.
_Avoid_: heal button, second ability

### Per-player state

**Class choice**:
The single class a player has selected, or none. It is the only per-player fact that is saved, and it
survives relog and respawn. Choosing a locked, unknown or missing class is rejected and leaves the
previous choice untouched; a bad or unknown id reads back as none.
_Avoid_: selection (ambiguous with the picker's selection), equipped class

**Class choice module** (`PlayerClasses`):
The module that owns the class choice and nothing else: the id, validation, and the round-trip that
saves it to player data under `InfusedClass`. It reads no clock and holds no transient state, so what
it does is assertable without a game: `writeId`/`readId` for the save string, `trySetClass` for
validated picks (unknown, missing and locked ids yield null), `currentDefinition` for the pure
current class, `copyFrom` for the respawn copy, which copies the choice and nothing else.
_Avoid_: player classes, player state (both still suggest transient state; that is the transient
runtime's job)

**Choice access** (`PlayerClassAccess`, `mixin/PlayerMixin`):
The one thing the mod needs from a player: the class it chose. The mixin implements the access on
every player and holds one choice slot per player, saved with player data (`ValueInput`/
`ValueOutput`), so the choice — and only the choice — lives on the player. The cast is the only way
for the server entry to reach a field a mixin added.
_Avoid_: player hook, mixin state

**Transient runtime** (`PlayerRuntime`):
The per-player stacks and cooldowns that last only as long as the session. It is handed the class they
belong to and the current time on each call, so it holds neither a clock nor a reference to anyone's
choice, and it is owned per player by the server entry rather than stored on the player. Stack
storage (counts plus last-trigger timestamps in definition-keyed maps) lives in `PassiveStacks`:
`stacks` reads one passive, `onTrigger` bumps
every passive on the class whose trigger matches (unknown triggers, a missing class and trigger-less
classes are inert, each bump re-stamps grace), `tickDecayMoved` drops every stack of each
passive on the class past its own grace, and reports the set that moved. `clear` drops one class's
stacks; cooldowns go through the lane verbs, armed only by ability `fire` and reset by the adapter
on switch. Callers and tests cross this module's seam; the stacks module is its implementation.
_Avoid_: player state, live state, runtime (too generic on its own)

**Transient runtime store** (`PlayerRuntimes`):
The one module that owns the transient runtime's lifetime: one per UUID, created on first use
(`runtime`), dropped on logout (`remove`), empty again after a respawn (`respawn`, which removes
before creating because both bodies share one UUID). Assertable with UUIDs alone, no player needed.
_Avoid_: runtime map, session cache

### Presentation

**Card line** (`ClassPresentation.CardLine`):
One line of a class card's body, carrying its text, the section it belongs to, and the colour and
weight it draws with.
_Avoid_: row, string, label

**Line kind** (`ClassPresentation.LineKind`):
Which section a card line belongs to: a passive or ability header, a passive or ability item, or the
notice. Kind is what decides style; a line's text never does.
_Avoid_: type, category

**Line style** (`ClassPresentation.LineStyle`):
The colour and boldness a line draws with, decided by its line kind alone: headers white bold
(`0xFFFFFFFF`), passive items light gray (`0xFFBDBDBD`), ability items blue (`0xFF7ED6FF`), the
notice title gold bold (`0xFFE8C15A`).
_Avoid_: format, theme

**Notice**:
The "coming soon" line a locked class's card ends with. It is recognised by its line kind, never by
matching the rendered text.
_Avoid_: magic string

**Card lines** (`ClassPresentation.cardLinesFor`):
The lines a card body shows. A locked class shows `Passives: ???`, `Active: ???` and `COMING SOON`;
an unlocked class shows a `Passives` header plus each passive's name (HP-scaled passives
sharing a group read as one line), or the
header plus `No passives yet` when it has none, and an `Active` header plus one line per active.
_Avoid_: card text

**Card tooltip** (`ClassPresentation.tooltipFor`):
The select button's tooltip. A locked class reads `Coming soon`; an unlocked class joins its
passives (`Passive: <name> — <picker copy>`) and one `Active: <name> — <description>` entry
per active, and ends `Click to choose <display>.`
_Avoid_: hover text

**Picker subtitle** (`ClassPresentation.subtitle`):
The line under the picker title: `Your choice shapes your ability`.
_Avoid_: header

**Card accent** (`ClassPresentation.accentFor`):
The colour of a card's top strip: the class's own accent, grey (`0xFF555555`) for a locked class.
_Avoid_: highlight

**Picker button** (`ClassPresentation.ButtonData`, `buttonFor`):
A card's button label and active state: `Locked`/inactive for a locked class, `Select`/active
otherwise. Its rect comes from the card renderer's button math, never the screen's.
_Avoid_: select button

**Card model** (`ClassPresentation.CardModel`, `cardFor`):
The whole card in one decision: lines, style, accent, button and tooltip together, so the locked
look is decided once instead of in five ternaries across callers.
_Avoid_: card DTO, view model

**Picker screen** (`client/ClassSelectScreen`):
The picker itself: dimmed backdrop, one card per class in a row, one `Button` per card with its
tooltip and active state, title, subtitle and the `Click a class to choose it` footer. It positions
cards with the renderer's row math, sizes buttons with its button rect, and passes hover through;
how a card draws is the card renderer's business, and what a card says is the card model's. It never pauses and never closes on Esc.
_Avoid_: GUI, menu

**Card renderer** (`client/ClassCardRenderer`):
Everything one class card looks like: body, accent strip, border, hover highlight for unlocked
cards, veil for locked ones, and the name, tagline, divider and card lines. It also owns the row
math (width, x, top), the button rect and the hover hit-test the screen positions cards with.
Style is resolved once per card (`cardFor` for the whole card, `cardStyleFor` inside frame/body);
the screen passes hover, never a style. Each line draws with
the colour and weight it carries, so nothing here reads what a line says. Cards are 120x156 with
10 inner padding and a 20-high button.
_Avoid_: card widget, draw helper

### Protocol and commands

**Select intent** (`SelectClassPayload`):
The client's `infusedclasses:select_class` intent: "I picked this class". The server validates it
against the choice module; a rejected pick leaves the picker open for retry. An accepted switch
clears what the old class earned and resets the cooldown, so no stale modifier survives.
_Avoid_: select packet

**Rush intent** (`RushPayload`):
The client's `infusedclasses:rush` intent: "I pressed the Rush key", sent while
the `R` key (`key.infusedclasses.rush`) is consumed each client tick. The server
validates class and cooldown; rejected presses leave the lane, the window and the
bonus untouched.
_Avoid_: shout packet, key packet

**Rend intent** (`RendPayload`):
The client's `infusedclasses:rend` intent: "I pressed the Rend key", sent while
the `G` key (`key.infusedclasses.rend`) is consumed each client tick. The server
validates class, stacks and cooldown; rejected presses leave the cooldown untouched,
and the empower lands on the next melee hit inside the window.
_Avoid_: heal packet, key packet

**Picker signal** (`OpenClassScreenPayload`):
The server's `infusedclasses:open_class_screen` signal telling the client to open the picker. Sent
once, on first join when the player has no class, and on demand by the select command.
_Avoid_: open packet

**Pick confirm** (`ClassConfirmedPayload`):
The server's `infusedclasses:class_confirmed` reply. The client closes the picker only on this, so
a rejected pick stays open for retry.
_Avoid_: confirm packet

**Class command** (`ClassCommands`):
The gamemaster-only `/class` wiring. `/class set <player> <class>` suggests registered ids, parses
one word and validates in the choice module (`Unknown or locked class` on failure); an accepted
switch clears what the old class earned like the picker path does; `/class select
<player>` is testing-only and reopens the picker for GUI iteration without bypassing the
locked-class check.
_Avoid_: admin command, cheat

## Decisions

Recorded in `docs/adr/`. `0001` is the decide-then-apply rule these terms are named after; `0002`
owns the choice/transient split; `0003` defers the class-lookup seam until a second source of
classes exists; `0004` inverts the Juggernaut kit to high-HP rewards (Vigor plus Momentum, Rush engage
plus Rend empower) with sound replacing root; `0005` moves the hit-gating decision out of the
server entry, the stack storage out of the transient runtime, and the trigger scan onto the class.
