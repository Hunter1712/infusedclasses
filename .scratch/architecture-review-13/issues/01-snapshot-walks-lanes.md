# 01 — The snapshot walks the lanes

Status: done

## Context

`PlayerStatusPayload` states its 23 facts six times: the record components, `of`'s
constructor call, the `FIELDS` table, `decode`'s positional reads, `writeValue`'s type switch,
and two test fixtures (`StatusHudTest.Snap` with 23 fields, `AbilityRosterTest.windows` with
23 arguments). The `Field` kind triad (`ArmedField`/`ExactField`/`IgnoredField`) exists to
type the movement rule per row, and `movedSince` walks it. A seventh window edits all five
production surfaces plus both fixtures. Round-13 candidate 1 (lands with candidate 2, which
gives the lanes a key to walk).

## Deliverable

- The record becomes `classId`, `Map<Lane, LaneRead> lanes`, `flipTarget`, `rouletteOutcome`,
  `momentumStacks`, `momentumGraceRemainingMs`, `momentumGraceStampMs`, `stance`, `streak`.
- `of` copies the state's lane map; `encode`/`decode` loop over `Lane.values()` (one long,
  one long, one boolean per lane) with the nullable enums by name; `movedSince` compares the
  class id, every lane's armed remainders and pending flag, then the discrete facts, still
  ignoring the grace remainder that derives from the compared stamp.
- The `Field` interface and its three records die; `writeValue`'s reflective switch becomes
  three typed writes (`writeStance`, `writeOutcome` and the loops).

## Done when

- `ProtocolTest`'s populated round trip still pins every lane's three reads and every discrete
  field with all-distinct values, and `PlayerStatusTest`'s movement cases still catch a lane
  armed, a one-shot spent, a window expired, a stance flipped, an outcome set, a Streak move,
  a class switched and a grace re-stamp at the cap.
- A quiet second still reads as no move (raw remainders never compared).
