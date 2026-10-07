# 04: Registry copy, docs, and in-game verify

Spec: `.scratch/berserker-bloodlust/spec.md`

**Blocked by:** 03 (needs the wired ability).

**Status:** ready-for-human

## Task

Player-visible copy plus the manual pass no test can cover:

- `ClassRegistry` Berserker actives `[bloodlust, second_wind]` with copy built
  from the 02 constants (Bloodlust 10s window / 30s lane / under-5-hearts gate
  / `bucket × 2%` / 2 HP per hit / builder `bucket` stacks / 1s root).
  Second Wind entry and numbers unchanged.
- `GLOSSARY.md`: War Cry→Bloodlust (ability + intent entries), Berserk→Fury,
  Warrior→Berserker leftovers from 01 if any; cooldown entry covers the
  Bloodlust 30s lane.
- `gradle test` green, then `gradle deployClient` (never `compileJava` alone
  for client-visible work) and verify in game via `/class select <player>`:
  `R` above 5 hearts fizzles free; `R` under 5 hearts roars, roots 1s,
  grants bucket-scaled Bulwark, heals on melee for 10s; `G` shield path
  unchanged; old `warrior` NBT pick loads as Berserker.

## Acceptance

- Copy numbers match constants (no drift); manual checklist above passes.
- No behaviour-code changes beyond copy in this ticket.

## Comments

Implemented 2026-10-07 (ponytail: full): all four tickets in one green sequence.
`gradle test` green (107 tests), `gradle deployClient` copied the jar to the
live 1.21.11 instance. Manual checklist left for human: `/class select <player>`,
`R` above 5 hearts fizzles free, `R` under 5 hearts roars + roots 1s + grants
bucket Bulwark + heals on melee for 10s, `G` shield unchanged, old warrior NBT
loads as Berserker (review fixes applied: applier-routed heal, derived copy, FuryTest rename, bucket-0 case, honest fire, berserker helpers).
