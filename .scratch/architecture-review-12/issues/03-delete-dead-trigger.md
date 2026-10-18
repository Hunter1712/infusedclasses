# 03 — Delete the trigger no adapter feeds

Status: done

## Context

`ON_HIT_TAKEN` is advertised — a constant, a phrasing branch, a glossary line — but no adapter
calls `onTrigger` with it since Bulwark left the build; its only producer is a
test-constructed passive. A future hit-taken passive would earn nothing silently (the
ADR-0005 failure mode). Round-12 candidate 3.

## Deliverable

- `ConditionalPassive`: the constant and its phrasing case are deleted; `ON_HIT_DEALT`'s copy
  stops referencing it.
- `GLOSSARY.md`: the trigger line drops the hit-taken half; `ClassRegistryTest`'s hit-taken
  copy test goes with the constant.

## Done when

- Nothing in `src/` or the glossary names `on_hit_taken`; the suite is green.
