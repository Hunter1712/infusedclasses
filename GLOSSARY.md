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
changed what should be on the player, so it needs no attribute write.
_Avoid_: passive system, stat updater

**Applied outcome** (`WarCryAbility.Outcome`):
The description an ability decision returns of what should be applied to the player: which effect, for
how long, at what amplifier, plus the sound and particle that go with it. War Cry's is Strength II
for 160 ticks at amplifier 1, with `minecraft:strength`, `minecraft:entity.ravager.roar` and
`minecraft:angry_villager`. It carries registry ids rather than registry objects, so it can be
produced without a running game. It is the part of an ability result that gets applied; the gating is not.
_Avoid_: ability result (that is the whole decision, gating included), buff

**Ability applier** (`AbilityApplier`):
The adapter that is the single place an ability outcome reaches the player: effect, sound and the
24-particle flat ring, plus the announcement on the action bar. A result with no outcome and nothing
to say — a wrong-class or on-cooldown press — is a no-op.
_Avoid_: ability system, effect handler

**Chat line** (`WarCryAbility.ChatLine`):
The text and the chat style an ability decision returns for the server entry to send verbatim,
including the remaining-seconds message for a press on cooldown. War Cry's fire line is `War Cry!`
in red; its cooldown line is `War Cry: %.0fs` with no style of its own, rounded with `%.0f`. Its
wording, rounding and locale are decided with the ability, not formatted by whoever sends it.
_Avoid_: message, notification

**Picker copy** (`ConditionalPassive.shortDesc`, `fullDesc`):
The decision of what a conditional passive's picker copy says, generated from its own numbers so it
cannot drift from behaviour. The card body uses the short form (`+1 armor per stack`); the tooltip
uses the full form. Only the trigger the mod emits is described as an event ("Taking a hit
grants 1 stack" for `on_hit_taken`); any other trigger reads as the bare trigger. Whole numbers read
as `1`, not `1.0`, and the decay rate is pluralised (`1 stack`, `2 stacks`).
_Avoid_: description, lore text

### Classes, passives and stacks

**Class** (`ClassDefinition`):
A playable archetype, registered once with its id, display name, locked flag, passives, active
ability id, accent colour, tagline, ability name and ability description. A class is data: it carries
no player and no state of its own. Warrior is the one playable class today: `Warrior`, War Cry
(`infusedclasses:war_cry`), accent `0xFFE74C3C`, tagline `Frontline bruiser`, ability `War Cry`
with `Gives Strength II for 8s`.
_Avoid_: character class definition, build

**Locked class**:
A registered class that cannot be chosen yet. It carries no passives and no ability, and the registry
rejects a locked definition that does; a playable definition without an ability is rejected the same
way. A locked class still carries picker text — accent, tagline — and is still shown in the picker.
Ranger and Mage are the two locked classes today.
_Avoid_: unavailable class, future class, hidden class

**Conditional passive** (`ConditionalPassive`):
A passive that earns stacks when a named trigger fires, rather than always being on. It carries its
own numbers — stack cap, attribute, per-stack value, grace period, decay rate — and its picker copy
is generated from those numbers so the copy cannot claim something the behaviour does not do.
Bulwark, the Warrior passive, is `on_hit_taken`, max 10, `minecraft:armor` under
`infusedclasses:bulwark_armor`, +1.0 per stack, 5s grace, 1 per second.
_Avoid_: buff, stackable stat

**Stack**:
One earned increment of a conditional passive, counted per player and capped at that passive's
maximum. Stacks are keyed by the passive definition, never by its name, so a passive is addressed
by what it is rather than by what it is called. They decay after a grace period and are transient:
they do not survive relog or respawn.
_Avoid_: charge, level, rank

**Cooldown** (`Cooldowns`):
The wait after an ability fires, tracked per player, also transient. War Cry's is 30s
(`WAR_CRY_MS`): `useWarCry` fires only when ready and reports it, `warCryRemaining` reports the
milliseconds left, 0 when ready.
_Avoid_: timer

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
choice, and it is owned per player by the server entry rather than stored on the player. Stacks live
in definition-keyed maps with last-trigger timestamps: `stacks` reads one passive, `onTrigger` bumps
every passive on the class whose trigger matches (unknown triggers, a missing class and trigger-less
classes are inert, each bump re-stamps grace), `tickDecay` ticks every passive on the class past its
own grace at its own rate (`(now - last) / 1000` past grace, times decay per second), advancing
the last-trigger clock by the stacks decayed so an overdue second is never recounted, and reports
whether any stacks moved.
_Avoid_: player state, live state, runtime (too generic on its own)

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
an unlocked class shows a `Passives` header plus each passive's name and short description, or the
header plus `No passives yet` when it has none, and an `Active` header plus the ability name.
_Avoid_: card text

**Card tooltip** (`ClassPresentation.tooltipFor`):
The select button's tooltip. A locked class reads `Coming soon`; an unlocked class joins its
passives (`Passive: <name> — <picker copy>`) and its active (`Active: <ability name> — <ability
description>`) and ends `Click to choose <display>.`
_Avoid_: hover text

**Picker subtitle** (`ClassPresentation.subtitle`):
The line under the picker title: `Your choice shapes your ability`.
_Avoid_: header

**Card accent** (`ClassPresentation.accentFor`):
The colour of a card's top strip: the class's own accent, grey (`0xFF555555`) for a locked class.
_Avoid_: highlight

**Picker button** (`ClassPresentation.ButtonData`, `buttonFor`):
A card's button label and active state: `Locked`/inactive for a locked class, `Select`/active
otherwise.
_Avoid_: select button

**Picker screen** (`client/ClassSelectScreen`):
The picker itself: dimmed backdrop, one card per class in a row, one `Button` per card with its
tooltip and active state, title, subtitle and the `Click a class to choose it` footer. It positions
cards with the renderer's row math and checks hover through it; how a card draws is the card
renderer's business. It never pauses and never closes on Esc.
_Avoid_: GUI, menu

**Card renderer** (`client/ClassCardRenderer`):
Everything one class card looks like: body, accent strip, border, hover highlight for unlocked
cards, veil for locked ones, and the name, tagline, divider and card lines. It also owns the row
math (width, x, top) and the hover hit-test the screen positions cards with. Each line draws with
the colour and weight it carries, so nothing here reads what a line says. Cards are 120x156 with
10 inner padding.
_Avoid_: card widget, draw helper

### Protocol and commands

**Select intent** (`SelectClassPayload`):
The client's `infusedclasses:select_class` intent: "I picked this class". The server validates it
against the choice module; a rejected pick leaves the picker open for retry.
_Avoid_: select packet

**War cry intent** (`WarCryPayload`):
The client's `infusedclasses:war_cry` intent: "I pressed the War Cry key", sent while the `R` key
(`key.infusedclasses.war_cry`) is consumed each client tick. The server validates class and
cooldown.
_Avoid_: ability packet, key packet

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
one word and validates in the choice module (`Unknown or locked class` on failure); `/class select
<player>` is testing-only and reopens the picker for GUI iteration without bypassing the
locked-class check.
_Avoid_: admin command, cheat

## Decisions

Recorded in `docs/adr/`. `0001` is the decide-then-apply rule these terms are named after; `0002`
owns the choice/transient split; `0003` defers the class-lookup seam until a second source of
classes exists.
