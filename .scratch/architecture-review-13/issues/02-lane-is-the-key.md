# 02 — The lane is the key

Status: done

## Context

`Cooldowns` hides a private `Lane` enum behind twelve per-lane verbs (`useRush` /
`rushRemaining` × six), `PlayerRuntime` repeats every lane again as a window field, a joint
arm verb and a consume verb (six + six + three + a hand-written reset array), and
`PlayerRuntime.State` names all six lane remainders and six window remainders as components.
The number of per-lane lists in the mod is the number of places a seventh window must be
written down; the comment at `Cooldowns.java:21-25` claims "a fifth lane adds one value plus
two one-line verbs" — the truth is eighteen declarations across six files. Round-13
candidate 2 (and the base for candidate 1).

## Deliverable

- `Lane` (new): the six lanes, each owning its cooldown millis (`cooldownMs()`), moved off
  `Cooldowns` so a lane is one row; `Cooldowns` becomes `use(lane, now)` /
  `remaining(lane, now)` / `reset()`.
- `LaneRead` (new): `(remainingMs, windowRemainingMs, windowPending)` — one lane's transient
  read, shared by `PlayerRuntime.State` and the snapshot.
- `PlayerRuntime`: `Map<Lane, Window>` replaces the six window fields; `arm(lane, now,
  windowMs[, payload])`, `rotate(now, landing)` and `roulette(now, outcome)` replace the six
  joint arms (the two landing-carrying windows keep their own arm, the rest share one);
  `consumeBonus(lane, now)` / `consumePayload(lane, now)` replace the three consumes;
  `resetAll()` replaces `resetCooldowns` and the reset array. `State` carries
  `Map<Lane, LaneRead>` plus the non-lane facts (Rend's unspent stamp, flip target, Roulette
  outcome, stance, Streak), with derived reads (`laneRemaining`, `lane`, `impetusPending`) so
  consumers keep naming facts, not map keys.
- `AbilityRoster.Row` becomes `(Lane lane, Identifier ability, String name, List<Rider>
  riders)`; both lane reads derive from the lane (`laneRemaining(state)` /
  `laneRemaining(status)`), and `View` dies with them.
- Abilities arm at their own fire sites: `RushAbility` names `WINDOW_TICKS * 50L`,
  `RendAbility` stamps its stack count, `RotateAbility`/`RouletteAbility` pass their landing,
  `AequilibriumAbility`/`AllInAbility` arm their lane.

## Done when

- No per-lane verb, field or reset line remains: a seventh window adds a `Lane` value, its
  cooldown in the same row, its window constant at its arm site, and one roster row.
- Every boundary the old verbs pinned still holds (`RushAbilityTest`, `RendAbilityTest`,
  `RouletteAbilityTest`, `AllInAbilityTest`, `LegionaryTest`, `CooldownsTest`,
  `PlayerRuntimeDeepeningTest`, `WindowFamilyTest`): lane independence, window expiry at the
  exact millisecond, joint arm/reject, reset wiping lanes, windows, landings and Streak.
- Tests cross lanes, never the map: no test constructs a `PlayerRuntime`'s window or reads
  `Cooldowns`' map; they go through the lane-keyed pair and the state read.
