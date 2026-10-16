# 05: Registry, copy, docs, in-game verify

Spec: `.scratch/gambler/spec.md`

**Blocked by:** 02, 03, 04 (needs every wired behaviour and its copy source).

**Status:** ready-for-agent

## Task

Turn the class on and prove it:

- Registry: `ClassRegistry` gains the Gambler id plus the two ability id
  constants (`infusedclasses:gambler`, `roulette`, `all_in`); the entry
  carries `[roulette, all_in]` with descriptions generated from the ability
  constants (under the usual active-copy budget), accent `0xFFD4AF37`, tagline
  `Double or nothing`, and its `CritRule`. The card shows `Crooked Dice` and
  `Streak` under `Passives`.
- Docs: README and PROJECT_DESCRIPTION class lists gain the Gambler and note
  the vanilla crit retirement; CHANGELOG entry (new class + mod-owned crits).
  ADR-0012 and the glossary (crit roll, crit rule, Crooked Dice, betrayal,
  Streak, Roulette, All In, Gambler) already landed — check they match the
  shipped behaviour, do not rewrite them.
- Tests: extend the registry and presentation tests (the entry, generated
  copy, the two passive card lines).
- Verify: `gradle test` green, then `gradle deployClient` (never
  `compileJava` alone) and the manual pass via `/class select <player>`:
  jump attacks no longer crit on any class, full swings occasionally do with
  the cue; Gambler betrayal cuts with its own cue; Streak loads per non-crit,
  resets on crit, locks 95/5 from streak 5, wipes on class switch and
  respawn; `R` spins each band readably on the 25s lane and rejects on
  cooldown; `G` gives 15s of ×2.0 crits on the 75s lane with `R` unaffected
  and betrayal unchanged; picker copy shows the Gambler page with no drift.

## Acceptance

- Copy numbers match constants (no drift); manual checklist above passes;
  `gradle test` green.
- No behaviour changes beyond registering the class (copy and wiring only).

## Comments

None yet.
