# Transient per-player state is owned by the server entry

## Status

Accepted.

## Context

One class held both halves of per-player state: the choice a player made, and the stacks and
cooldowns that only mean anything while they are playing. The live half read a clock, so the
persisted half was not obviously pure, and neither half could be asserted on its own — the stacks
tests reached through the choice, and the choice tests had to tolerate a clock.

Keeping the choice pure is not a preference. It is what makes "what does this class grant" a question
with a plain-JUnit answer, and it is what keeps the saved data the one thing that crosses a relog.

## Decision

Split per-player state at the seam it already drew, and give the transient half somewhere to live.

- `PlayerClasses` is the **class choice module**: the id, validation, and the save-data round-trip
  under `InfusedClass`. No clock reads, no transient state, and no reference to anything live.
  `writeId`/`readId` carry the save string (bad or unknown ids read back as none),
  `trySetClass` validates picks (unknown, missing and locked ids yield null and leave the previous
  choice untouched), `currentDefinition` answers the current class, `copyFrom` copies the choice
  and nothing else.
- `PlayerClassAccess` is the **choice access**: the one thing the mod needs from a player. The
  player mixin implements it on every player and holds the one choice slot, saved with player data
  (`ValueInput`/`ValueOutput`), so only the choice lives on the player.
- `PlayerRuntime` is the **transient runtime**: stacks, the cap, decay, the grace period and
  cooldowns (`Cooldowns`: War Cry 30s, `useWarCry`/`warCryRemaining`). It is handed the class they
  belong to and the current time on every call, so it holds neither a clock nor a player's choice,
  and it reads no Minecraft types beyond its own signature. Stacks and last-trigger timestamps are
  keyed by the passive definition, not its name. `onTrigger` bumps every matching passive and
  re-stamps grace; `tickDecay` ticks every passive on the class past its own grace at its own rate
  and reports whether any stacks moved, so a tick that moved nothing needs no attribute write.
- Because the transient half is no longer reachable from the player, the **server entry owns its
  lifetime**: one per player keyed by UUID, created on first use, replaced by an empty one on respawn,
  and removed on disconnect.
- Wiring, as it stands: join opens the picker when the player has no class and creates nothing;
  classless players stay inert on damage and on tick, costing no transient state. Damage feeds the
  `on_hit_taken` trigger only; the end-of-tick pass decays stacks and then applies the passive
  effect. On respawn only the choice is copied to the new player, and the old runtime entry is
  removed before the new one is created — both players share one UUID, so the other order would hand
  the respawned player the stacks their old body earned. That ordering lives in wiring no test
  covers, and is marked as such.
- Nothing transient crosses. On respawn only the choice is copied to the new player; the new player's
  stacks and cooldowns start empty, which is what a respawn has always done to them.

## Consequences

- Both halves are testable on their own terms, with time injected rather than waited on.
- **A missed removal resurrects that player's stacks and cooldowns on relog.** The map entry is the
  only thing keeping this state session-scoped; there is nothing behind it, and the entry class is
  wiring that no test covers. Removing the disconnect handler, or keying the map on anything other
  than the player's UUID, silently makes a player keep their stacks and their cooldowns across
  sessions.
- Cooldowns therefore reset on relog today. That is a known trade, not an oversight: if it ever
  becomes an exploit, persisting cooldowns is a deliberate change to the choice module's save data,
  not a bug fix.
- Two places now know about a player's state — the mixin for the choice, the entry class for the
  transient half. That is the cost of keeping the choice free of transient state, and one of them is
  where the persistence hook already lived.
