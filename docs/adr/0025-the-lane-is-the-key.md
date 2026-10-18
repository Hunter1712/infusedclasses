# ADR-0025: The lane is the key

Status: accepted

## Context

The window state had grown as six parallel facts. `Cooldowns` held one length constant and
one verb pair per lane (`RUSH_MS`, `useRush` / `rushRemaining`, …); `PlayerRuntime` carried six
joint arm methods, six consumes and a hand-written reset array; `PlayerStatusPayload` stated
23 per-lane components plus a `Field`/`ArmedField`/`ExactField` table mapping them back to
lanes for `of`, `encode`, `decode` and `movedSince`; and the per-window read verbs
(`rushWindowRemaining`, `rendRemaining`, …) spread the same three rules — a cooldown
remainder, a window remainder and a pending flag — across every caller.

Round 13 (candidate 2, its top pick) observed that the lane was the key all along: the field
tables, the verb sets and the reset array were one enumeration spelled out by hand, and a
seventh window cost edits in the snapshot, the codec, the runtime, the cooldowns and the
tests before it cost a behaviour (about nine edits in two files).

## Decision

1. **`Lane` is a top-level enum** with the six values (RUSH, REND, ROTATE, OATH, ROULETTE,
   ALL_IN), each owning its cooldown length (`cooldownMs()`). The lengths move off
   `Cooldowns`; the window durations stay with their abilities.
2. **One lane-keyed vocabulary.** `Cooldowns` is a `use(lane, now)` / `remaining(lane, now)` /
   `reset()` triad; `PlayerRuntime` holds an `EnumMap<Lane, Window>` and exposes one
   `arm(lane, now, windowMs[, payload])` / `rotate(now, landing)` / `roulette(now, outcome)` /
   `consumeBonus(lane, now)` / `consumePayload(lane, now)` / `resetAll()` set in place of the
   six-per-lane verbs and the reset array.
3. **One lane read.** `LaneRead(remainingMs, windowRemainingMs, windowPending)` is the shared
   per-lane read; `PlayerRuntime.State` and `PlayerStatusPayload` each carry a
   `Map<Lane, LaneRead>` and derive their reads over it (`lane(lane)`; the state also derives
   `laneRemaining(lane)`), and `of`, `encode`, `decode` and `movedSince` loop `Lane.values()`.
4. **Window durations come in at the arm.** Each ability's `fire` names its own window constant
   at its arm site (`arm(lane, now, X.WINDOW_TICKS * 50L)`), so the runtime holds no per-ability
   length — except the two landings that choose their own: `rotate` names `SWITCH_DEAL_MS` /
   `SWITCH_TAKE_MS` and `roulette` the spin's window at the landing's arm, the same place the
   landing arms its lane.
5. **The ROTATE lane's one-shot is derived, stated once.** Every arm stamps the window's
   pending flag, but `state(now)` reads a lane's one-shot through one rule — Rotate's counts
   only a landing on GLADIUS — so the Impetus read cannot be re-spelled by a caller.

## Consequences

- A seventh window is one `Lane` value plus its arm site: the snapshot, the codec, the runtime
  and the cooldowns grow no fields, and the codec's encode order cannot drift from its decode
  order because both are `Lane.values()`.
- ADR-0023 point 1's `View` is revised: a row carries its `Lane` and its riders spell the
  payload's lane reads (`windowRemaining(lane)`, `pending(lane)`, `pendingWindow(lane)`), so
  the pairing still has one home. Both lane reads stay split as 0023 point 3 says: the
  `State` server-side, the payload client-side, one row.
- ADR-0021's `consumeRendBonus` name retires into the lane verbs (`consumePayload(Lane.REND,
  now)`); its `peekRendBonusStacks` read survives as the state's `rendBonusStacks`, because the
  unspent Rend stamp is an amount, not a lane flag and not what `LaneRead` carries. The
  substance — window nouns carry the vocabulary — holds.
- The wire shape changes (23 components to nine); accepted: one jar ships both sides.
- Drift pins: `CooldownsTest` pins each lane's own length, `PlayerRuntimeDeepeningTest` the
  arm/consume/reset behaviour per lane, `ProtocolTest` a full payload round trip with
  all-distinct lane reads, `AbilityRosterTest` every registered active's row.
