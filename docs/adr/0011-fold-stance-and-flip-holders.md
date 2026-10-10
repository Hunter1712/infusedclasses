# ADR-0011: Fold the stance adapter and the flip holders home

Status: accepted

## Context

Round 9 found three modules failing the deletion test. `StanceApplier` (created
by ADR-0007.3) has 0 public verbs and 2 call sites, both in `PassiveApplier`;
its three enumerations of one modifier set are the write/remove/switch views of
one reconcile. `ImpetusAbility`/`TestudoAbility` are 24-line data holders with
no trigger/fire; the flip guard repeats in `MeleePlan`, `PlayerRuntime`, and
`ClassRegistry`, and the plan checks two ability ids for one Legionary class.

## Decision

1. **The stance adapter folds back into the passive applier.** `applyStance`,
   `removeStanceModifiers`, and the full modifier set become private
   implementation of `PassiveApplier`; the switch marker inlines into
   `onClassChanged`. This reopens ADR-0007.3 for the stance case only: one
   adapter was a hypothetical seam — the second adapter never arrived.
   `AttributeWriter` (four callers) stays split.
2. **Both flip buffs home on `RotateAbility`.** Mults, windows, names, and
   picker copy move; the holder files and the `IMPETUS_ABILITY` /
   `TESTUDO_ABILITY` ids are deleted. Legionary carries `[ROTATE,
   AEQUILIBRIUM]`; the melee plan gates every Legionary path on `ROTATE` and
   peeks the Impetus flag once per hit. ADR-0010 behaviour (switch buffs off
   the flip record, one clean ultimate) is unchanged, only homed.

## Consequences

- A third melee class still adds registry entries plus plan/morph rows; the
   entry, the applier, and the receivers stay untouched.
- `MeleePlanTest` / `LegionaryTest` pin the Rotate-homed constants; the
   four-active Legionary assertion becomes two.
- Cost: `PassiveApplier` regrows the stance reconcile it held before ADR-0007.
   It passes the deletion test now: deleting it regrows orchestration across
   the entry, the tick, and the switch.
