# Decide purely, apply in an adapter

## Status

Accepted. Supersedes nothing; `docs/adr/0003` is the one decision this one deliberately defers.

## Context

The mod entry class had grown because decisions were interleaved with the code that touches the game:
what an earned stack is worth to the player's armor, what an ability press should do to them, and how
a card line should draw. Each was a small calculation buried in a method that also had to reach a
player, a level or a graphics object, so none of them could be asserted without a running game.

Three of these already had the same shape. The problem was that nothing said so. Left unstated, they
read as three unrelated seams, which is exactly how a fourth gets invented the next time something new
needs deciding.

One constraint makes the rule more than a style preference: touching a registry class outside a
bootstrapped game throws. A decision that named an effect by its registry object could not be
constructed at all in a test, so the decisions have to name effects, sounds, particles and attributes
by id and key.

## Decision

One rule, stated once:

> A decision module takes data and returns a description of an outcome. An adapter applies it.

The seam is drawn at the highest point that stays pure. A decision must not need a player, a level, an
attribute instance or a graphics object, so it names registry things as ids and keys
(`Identifier`, `ResourceKey`) and the adapter is what resolves them.

Four instances of this rule exist, and they are four instances of one rule, not four separate
seams:

- The **passive effect** (`PassiveEffect`) decides which attribute modifier a passive's stacks are
  worth. `PassiveApplier` applies it: writing only when the applied value differs from the wanted
  one, and removing only when stacks moved.
- The **applied outcome** (`RushAbility.Result` plus `RendAbility.Result`, each with its `Kind`,
  `remainingMs` and chat line — Rend also carrying `empowerHp` and `consume`) decides what an ability press
  should do. `RushApplier`/`RendApplier` apply it — sharing announcement, holder lookup and cast
  fanfare through `AbilityApplier` — and
  the server entry maps one result to one `apply` call.
- The **picker copy** (`ConditionalPassive.fullDesc`) decides what a passive's picker copy says,
  generated from its own numbers. The tooltip and the tests read it; nobody rewords it.
- The **card presentation** (`ClassPresentation`) decides everything a card shows from the
  definition: line style per line kind, card lines, tooltip, subtitle, accent and button data.
  The card renderer draws it without reading what a line says.

An addition follows this shape rather than inventing an interface for itself. If a new decision needs
a game object to describe its answer, the seam is in the wrong place — move the boundary down, do not
add a rule.

## Consequences

- The test suite stays a description of behaviour: four decision surfaces to assert through, and no
  new one per effect. Each is assertable in plain JUnit with no game bootstrap.
- A rejected decision is an absent answer, not a sentinel: no modifier at zero stacks, no outcome on a
  wrong-class or on-cooldown press. An adapter can therefore stay trivial, and a null answer needs no
  special handling. A result with no outcome and nothing to say is a no-op.
- Decisions own their own numbers. The stack-to-attribute value, the effect duration and amplifier
  (Speed I, 80 ticks; Rend empower `6 + 1/stack` within 120 ticks), the sound ids, the cooldown wording and its rounding, the
  passive copy wording, and the per-line colour and weight are all facts a test can pin,
  where before they were constants scattered through the code that applied them.
- The cost: every effect has to be expressed as data and ids, and each adapter carries the resolution
  code. A new effect is a definition plus resolution in the adapter, not a new seam.
