# 05 — The switch cleanup is a decision

Status: done

## Context

`PassiveApplier.onClassChanged` mirrors the tick: one loop for the old class's stack
passives, one for the Vigor pair, one for the window-aura families, each repeating the
tick's write side as a remove side. The mirror has already drifted from the tick's order (the
tick runs decay, conditionals, Vigor, families; the cleanup runs passives, Vigor, families),
and the whole rule needs a `ServerPlayer` to run, so no test can pin which modifiers a switch
takes off — the one reconcile step that cannot be asserted. Round-13 candidate 5.

## Deliverable

- `PassiveApplier.cleanupFor(ClassDefinition oldDef, ClassDefinition newDef)` — a pure
  decision answering every modifier a switch must take off: the old kit's stack passives and
  Vigor pair (`PassiveEffect.forStacks`/`forBucket` at a nonzero value), plus each window
  family's whole modifier list when the old class holds its marker and the new one does not,
  deduplicated by modifier id and keyed on what the new class writes.
- `onClassChanged` becomes: remove the decided list, clear the old stacks, reset the runtime.
  The first of the two `def == null` guards dies with the mirror (the list is empty for a null
  old class).
- The families' `writtenBy` half is shared with the cleanup, `FAMILIES` stays the one table.

## Done when

- A new test pins the list for Juggernaut→Legionary (Momentum's armor, both Vigor modifiers
  and the swept Rush window's speed — the id-level rule, families included), Juggernaut→Juggernaut
  (empty), Legionary→Gambler (the stance auras plus the Aequilibrium window pair),
  Gambler→Legionary (the wheel's Ward/Haste pair), null→X (empty) and X→null (the whole kit).
- The count of remove loops in `onClassChanged` is zero.
