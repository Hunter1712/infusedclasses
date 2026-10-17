# Spec: Ponytail audit cleanup 02 (vestigial surface)

Status: done

## Problem Statement

The full-codebase ponytail audit (2026-10-16) found vestigial surface left by
the class evolution: a lock flag no class sets and no path reads, a picker
button record whose active field has no reader, four production APIs only tests
call, a zero-valued damage base, a pre-release class-id remap, a registry
literal that bypasses the protocol constant, and a mod description that missed
the newest class. No player-visible behaviour was wrong; the cost is a
maintainer guessing which of two shapes is canonical.

## Solution

Deletion only, no player-visible behaviour change. The existing suite is the
harness; no new tests were needed.

## Implementation Decisions

- `ClassDefinition.locked` deleted (field, three registration args, the
  `trySetClass` check). The word survives only in the rejection wording
  (`Unknown or locked class`), kept as the established player-facing string.
- `ClassPresentation.ButtonData` deleted with `buttonFor()` and the
  `CardText.button` field; `ClassSelectScreen` renders the literal `Select`.
  The control returns when a class can actually be locked.
- `MeleePlan.dealFor` deleted: production read the plan's `deal`; tests go
  through `planHit(...).deal()`.
- `PlayerRuntime.lastTrigger` / `PassiveStacks.lastTrigger` deleted: decay
  reads the map directly and no production caller existed.
- `HpScaledPassive.name` deleted: the card groups by `group`; the field only
  fed assertions.
- `RendAbility`: `BASE_HP` deleted (the base is zero, always), and the `Result`
  drops `empowerHp` — the fire stamps the stack count on the runtime and hit
  time recomputes the amount from it; the fire-time copy had no reader.
- `PlayerClasses.readId` drops the warrior/berserker remap: pre-release ids
  never shipped; a stale id now reads as no class and the picker re-asks.
- `ClassRegistry` identifier literals route through one `MOD_ID` holder: `ClassPackets.MOD_ID`, later hoisted to `InfusedClasses.MOD_ID` and `ClassPackets` deleted.
- `fabric.mod.json` description names all three classes.
- GLOSSARY synced: Class, Locked class, Class choice module, Applied outcome,
  Card text, Picker button. `docs/guide/commands.md` drops its lock wording;
  ADRs stay as point-in-time records.
- Incidental nits from the same audit ride along: `ConditionalPassive.word()`
  loses its duplicate javadoc (kept on `wordFor`), `LegionaryTest`'s
  cooldown-result local renames `locked` → `onCooldown`, and the
  `fabric.mod.json` description clauses tighten to fit all three classes under
  the listing length.

## Out of Scope

- `PlayerRuntimes` keeps `ConcurrentHashMap`: correct on edge cases at zero
  cost, and a plain `HashMap` buys nothing.
- Payload-per-file, lane verbs, decide-then-apply records: documented keepers.
- `MeleePlan.HitPlan.deal`/`take` stay: the plan's named parts are the decision
  record's shape.
- Message wording `Unknown or locked class` unchanged.
- ADR-0004's load-remap line (`berserker`/`warrior` ids map forward) is thereby
  reversed; ADRs are records, not living docs, and 0.x allows class resets with
  no migration until beta.
