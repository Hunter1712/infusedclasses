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

**Applied outcome** (`WarCryAbility.Outcome`):
The description an ability decision returns of what should be applied to the player: which effect, for
how long, at what amplifier, plus the sound and particle that go with it. It carries registry ids
rather than registry objects, so it can be produced without a running game. It is the part of an
ability result that gets applied; the gating is not.
_Avoid_: ability result (that is the whole decision, gating included), buff

**Chat line** (`WarCryAbility.ChatLine`):
The text and the chat style an ability decision returns for the server entry to send verbatim,
including the remaining-seconds message for a press on cooldown. Its wording, rounding and locale are
decided with the ability, not formatted by whoever sends it.
_Avoid_: message, notification

### Classes, passives and stacks

**Class** (`ClassDefinition`):
A playable archetype, registered once with its passives, its active ability and its picker copy. A
class is data: it carries no player and no state of its own.
_Avoid_: character class definition, build

**Locked class**:
A registered class that cannot be chosen yet. It carries no passives and no ability, and the registry
rejects a locked definition that does. A locked class is still shown in the picker.
_Avoid_: unavailable class, future class, hidden class

**Conditional passive** (`ConditionalPassive`):
A passive that earns stacks when a named trigger fires, rather than always being on. It carries its
own numbers — stack cap, attribute, per-stack value, grace period, decay rate — and its picker copy
is generated from those numbers so the copy cannot claim something the behaviour does not do.
_Avoid_: buff, stackable stat

**Stack**:
One earned increment of a conditional passive, counted per player and capped at that passive's
maximum. Stacks decay after a grace period and are transient: they do not survive relog or respawn.
_Avoid_: charge, level, rank

**Cooldown**:
The wait after an ability fires, tracked per player, also transient.
_Avoid_: timer

### Per-player state

**Class choice**:
The single class a player has selected, or none. It is the only per-player fact that is saved, and it
survives relog and respawn. Choosing a locked, unknown or missing class is rejected and leaves the
previous choice untouched.
_Avoid_: selection (ambiguous with the picker's selection), equipped class

**Class choice module** (`PlayerClasses`):
The module that owns the class choice and nothing else: the id, validation, and the round-trip that
saves it to player data. It reads no clock and holds no transient state, so what it does is
assertable without a game.
_Avoid_: player classes, player state (both still suggest transient state; that is the transient
runtime's job)

**Transient runtime** (`PlayerRuntime`):
The per-player stacks and cooldowns that last only as long as the session. It is handed the class they
belong to and the current time on each call, so it holds neither a clock nor a reference to anyone's
choice, and it is owned per player by the server entry rather than stored on the player.
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
The colour and boldness a line draws with, decided by its line kind alone.
_Avoid_: format, theme

**Notice**:
The "coming soon" line a locked class's card ends with. It is recognised by its line kind, never by
matching the rendered text.
_Avoid_: magic string

## Decisions

Recorded in `docs/adr/`. `0001` is the decide-then-apply rule these terms are named after.