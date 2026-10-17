# 02: Status sync at change points

Status: done

## Context

One snapshot payload is not enough: the client must be told when something it cannot derive changed, and must hear nothing on quiet ticks. The emission guard is `movedSince`; the adapter keeps one last-sent snapshot per player and rebuilds on demand.

## Goal

- Restore `StatusSync`: `maybeSend(player, now)` (rebuild, compare, send only on a move), `sendNow(player, now)` (join and respawn force one snapshot), `forget(uuid)` (logout, and the stale half of a respawn).
- Wire the call sites:
  - `ServerEvents.register`: join forces a snapshot; respawn forgets then forces; disconnect forgets.
  - `ServerEvents` server tick: after `PassiveApplier.apply` (the decay move lives there) call `maybeSend`.
  - Intent receivers: after each branch (rush/rotate/roulette signature; rend/oath/all-in ultimate) call `maybeSend`; a rejected press moved nothing and sends nothing.
  - `settleAttackerHit`: after the settle lands.
  - `ClassSwitch.trySwitch`: on an accepted switch only, so the picker path and `/class set` both emit through one seam.

## Done when

`gradle build` is green; a manual dev-client pass shows the stack appearing on join, updating on fires/settles, and hiding on classless players. No per-tick packet traffic while idle (the guard, not a timer, decides).

## Comments

Implemented: `StatusSync` (maybeSend/sendNow/forget) wired at join, respawn, disconnect, the post-decay tick, both intent receivers, `settleAttackerHit`, and `ClassSwitch.trySwitch` on an accepted switch. Build green; the live emission behaviour rides the same dev-client pass as issue 05.
