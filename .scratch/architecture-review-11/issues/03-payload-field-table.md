# 03 — One field table in the snapshot payload

Status: done

## Context

`PlayerStatusPayload` restates its 22 fields across the record, `of`, `movedSince`,
`encode` and `decode`; a new field edits all five. Round-10 flag 1 and round-11
candidate 3.

## Deliverable

- A private `Field<?>` table: extractor plus move rule — `ARMED` for decaying
  remainders, `EXACT` for discrete facts, `IGNORED` for the grace remainder (derived
  from the stamp, never compared).
- `encode` walks the table; `movedSince` walks the table. `decode` stays explicit and
  reads in the table's order (Java records have no table-driven construction without
  reflection; the round trip is the pin).
- ProtocolTest's populated round trip gets all-distinct values so a table/decode order
  drift fails; its `bare` fixture equals the new `classless()`.

## Done when

- Adding a field is: record component + table row + `decode` line (+ `of` line); a
  table row whose order `decode` misses fails the round-trip test.
