# Spec: Architecture review round 14 — implement the six deepening candidates

Status: implemented — picker hand check owed

## Goal

The Oct 19 2026 architecture review (report: `/tmp/opencode/architecture-review-20261019-172939.html`,
six candidates) lands as one commit on `main`. Every candidate's real win is a deletion: four
id-keyed tables for six abilities become one row table, the applier stops deciding, the status
payload stops restating the runtime schema, and the picker resolves one view per frame. Behavior a
player can see is unchanged — the wire shape, the HUD rows, the cooldown boundaries and the
attribute reconcile are pinned by the tests that already existed, plus the new ones below.

## Decisions

- **01 — fire from the row.** `AbilityRoster.Row` gains its `Slot` (signature/ultimate), its
  `boolean fans` and its pure `Fire` (the ability's own gate-and-arm `fire`, one ignored roll so
  all six share a shape). `BindMorph.SignatureTarget`/`UltimateTarget` and their two walks die:
  `slotFor(def, slot)` walks the class's actives to the first row of that slot (fallbacks Offload
  and Onslaught), and `shape(def)` draws the fired row only when the class carries it.
  `SlotPresses` collapses to one `press` over the slot: class read, classless bail, `row.fire`,
  one `announceAndCast` driven by `row.fans()`. ADR-0029 records it; ADR-0009 point 1 is
  superseded (the fire is pure — the player never enters the row — so the switch has no reason
  left to exist).
- **02 — the applier stops deciding.** `WindowFamily` becomes top-level and each window ability
  exposes its own `windowFamily()`: Rotate (both stances, current wanted), Aequilibrium (the oath
  window's plate, now its own family rather than riding the stance family), Roulette (the live
  spin), Offload (the loosened pair). The applier keeps only the row list and one reconcile loop.
  `ConditionalPassive` gains `barRows(stacks) -> modifiers` (empty by default); `Momentum.PASSIVE`
  passes `Momentum::forStacks`, so the tick loop reconciles every passive through
  `barRows` and the `Momentum.PASSIVE.equals` dispatch dies. ADR-0030 records it.
- **03 — one facts value for the wire.** `PlayerRuntime` gains `BarRead(stacks, graceRemainingMs,
  graceStampMs)` and the one `bar(passive, now)` read; the `graceRemaining`/`graceStamp`
  forwards die (`stacks` stays: the applier and the melee plan read it).
  `PlayerStatusPayload` becomes `(classId, PlayerRuntime.State state, PlayerRuntime.BarRead
  bar)` with the flat accessors kept for consumers; `State.movedSince(prev)` owns the
  armed-flag comparison, and the payload's guard is class id + bar count/stamp + the state's own
  move. ADR-0031 records it.
- **04 — the tick sequence is named.** `TickPass.run` owns read → apply → drained-Onslaught end →
  re-read only when it ended → snapshot; `ServerEvents` loops the players and calls
  `StatusSync.maybeSend(player, snapshot)`. The hit path needs no twin pass: `MeleeHook` already
  routes one hit through one method (`MeleePlan.resolve` then `DamageSettle`), so the tick's
  re-read was the only sequence the wiring had to remember.
- **05 — one resolved card per frame.** `ClassSelectScreen` resolves one `View` (card rectangle,
  `CardModel`, measured layout) per frame in the background pass and reuses it in the body pass;
  `lastMaxScroll` disappears and the wheel clamps against the same view's measure.
- **06 — one answer envelope for every fire.** `RotateAbility.fire` and `RouletteAbility.fire`
  return `AbilityOutcome` like the other four; their `trigger` keeps the `Result` records and the
  runtime state remains the one read of what landed. No `Result` type crosses a slot press.
  ADR-0029 records it.

## Testing

- New pins: rows carry their slot; a row's fire runs the ability it names (Rotate flip, Roulette
  band); the payload equals the state and bar read it was built from; `Momentum.PASSIVE.barRows`
  maps counts to its rows; a class carrying Aequilibrium without Rotate sweeps its plate.
- Re-pointed pins: `BindMorphTest` (rows, not targets), `WindowFamilyTest` (ability rows),
  `OffloadAbilityTest`, `LegionaryTest`/`RouletteAbilityTest`/`AllInAbilityTest` (envelope fires),
  `PlayerStatusTest`/`StatusHudTest`/`StatusStoreTest`/`ProtocolTest` (composed payload).
- Full suite: 261 tests, 0 failures.

## Hand check owed

The picker is client-visible: reopen it in game (deployed jar) and read the instance log; card
content, scroll clamping and paging must read unchanged.
