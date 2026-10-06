# 04 — Copy, docs, verification

Status: done

Spec: `.scratch/warrior-balance/spec.md`

Blocked by: 01, 02, 03

## Task

- Picker copy regenerates from constants (no hand-written numbers):
  `ClassDefinition.ActiveAbility` descs (War Cry 10s/20s + builder line, Second
  Wind absorption shield), `ConditionalPassive.fullDesc` (20-stack Bulwark),
  `HpScaledPassive.fullDesc` (new per-steps), `fabric.mod.json` description
  (was "Second Wind heal" — now false, reworded to the opener/builder/spender loop).
- `GLOSSARY.md`: update Class / Bulwark / Berserk / War Cry / Second Wind /
  Cooldown entries (bonus HP gone, 20×0.5, 0.3/0.05, builder shout, absorption
  spender, 20s lane).
- Verify: `gradle test`, then `gradle deployClient` and in-game pass
  (`/class select <player>`): R opens + seeds 5, hits build toward 20, G shields
  at low HP with Berserk live, 5s idle drops all.

## Acceptance

- No stale numbers in picker/tooltip/glossary (grep for `60s` War Cry, `+20`,
  `1.0 HP per`, `0.4`, `0.08` — all gone or updated).
- `gradle test` green, client playtest done.

## Comments

None yet.
