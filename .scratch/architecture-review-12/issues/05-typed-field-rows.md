# 05 — The field table's rows are typed

Status: done

## Context

`Field<T>(get, move)` lets any row carry `Move.ARMED`, and `movedSince` compares armed rows
through a `(Long)` cast; a misplaced row compiles and only explodes when a player's two
snapshots are compared. The table is the right concentration — its one semantic rule ("ARMED
only on decaying remainders") is not in the type. Round-12 candidate 5.

## Deliverable

- `Field` splits into `ArmedField` (a decaying remainder, armed-vs-armed at one clock),
  `ExactField<T>` (equality), and `IgnoredField<T>` (written, never compared: derived from
  another field). Each row owns its compare and write half.
- Table order unchanged; `decode` order untouched.

## Done when

- The `(Long)` cast is gone; ProtocolTest's all-distinct round trip and the `movedSince` pins
  pass unchanged.
