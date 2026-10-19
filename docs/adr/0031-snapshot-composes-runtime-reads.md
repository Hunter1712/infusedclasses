# ADR-0031: The status snapshot composes the runtime reads

Status: accepted

## Context

The review's third and fourth candidates found the snapshot restating the runtime's schema: the
payload carried a lane map plus eight scalar facts that `PlayerRuntime.State` already answers,
plus three one-line forwards for the bar's count, grace remainder and feed stamp; `encode`,
`decode` and `movedSince` each re-spelled that field list. The tick sequence — one state read,
the applier, the drained-Onslaught end, a conditional re-read, the snapshot — lived inline in
`ServerEvents`, where the re-read was a thing the wiring had to remember.

## Decision

1. **One bar read.** `PlayerRuntime.BarRead(stacks, graceRemainingMs, graceStampMs)` with one
   `bar(passive, now)`; the `graceRemaining`/`graceStamp` forwards die and `BarRead.NONE` is the
   classless value.
2. **The payload composes what the runtime answers.** `PlayerStatusPayload(classId,
   PlayerRuntime.State state, PlayerRuntime.BarRead bar)`. The flat accessors stay for
   consumers; `of`, encode and decode spell the wire order once.
3. **`State.movedSince(prev)` owns the armed comparison.** The payload's guard is the class id,
   the bar's count and stamp, then `state.movedSince` — the discrete facts plus each lane's armed
   flags, never the raw remainders that decay on every rebuild. A seventh window still costs the
   payload nothing.
4. **`TickPass.run` names the tick sequence.** Read → apply → drained-Onslaught end → re-read only
   when that end closed the window → snapshot. `ServerEvents` loops the players and calls
   `StatusSync.maybeSend(player, snapshot)`; `maybeSend` takes the value the tick built instead of
   rebuilding it.

The hit path needs no twin pass: `MeleeHook.afterDamage` already routes one hit through one
method (`MeleePlan.resolve` then `DamageSettle`), so the tick's conditional re-read was the only
sequence the wiring had to remember.

## Consequences

- A snapshot field is added or renamed in one place; the guard and the codec move with it.
- `StatusSync.maybeSend` and the tick no longer each know the builder's inputs.
- `PlayerStatusTest` pins that the payload equals the state and bar it was built from; the
  protocol round trip still pins the wire order, which is byte-for-byte unchanged.
