# 04 — Class identity travels resolved

Status: done

## Context

The tick already holds `def` + `runtime`, then `maybeSend(player, now)` re-fetches both and
`PlayerStatusPayload.of(classId, ...)` resolves `ClassRegistry.byId` a third time just to find
the Momentum passive. Classless identity has two representations (a null definition and an
empty id string) kept in agreement by hand. Round-12 candidate 4.

## Deliverable

- `StatusSync.maybeSend(player, def, runtime, now)` — the tick passes what it holds; the
  forced `sendNow(player, now)` keeps resolving internally (join and respawn, classless
  included, no runtime materialised).
- `PlayerStatusPayload.of(ClassDefinition def, PlayerRuntime runtime, long now)` — no
  registry lookup; `def == null` stays the classless shape.

## Done when

- No registry lookup remains on the status path (the `of` body included);
  `PlayerStatusTest` call sites pass definitions; `classless() = of(null, null, 0)` still
  pinned.
