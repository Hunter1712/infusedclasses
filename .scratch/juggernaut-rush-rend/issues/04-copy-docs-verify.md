# 04: Registry copy, docs, and in-game verify

Spec: `.scratch/juggernaut-rush-rend/spec.md`

**Blocked by:** 03 (needs the wired abilities).

**Status:** done

## Task

Player-visible copy plus the manual pass no test can cover:

- `ClassRegistry` Juggernaut actives `[rush, rend]` with copy built from
  the 02 constants (Rush 4s Speed / 25s lane / cleanse-harmful / +3 bonus /
  horn; Rend next-hit empower 6s / `6 + 1/stack` / 60s lane / roar; no `Roots`
  text anywhere). Vigor + Momentum picker lines from their numbers.
- `GLOSSARY.md`: Berserker→Juggernaut, Fury→Vigor, Bulwark→Momentum,
  Bloodlust/Second Wind→Rush/Rend (ability + intent entries), Spellcast
  root→sound + cleanse; cooldown entry covers Rush 25s / Rend 60s lanes.
- New ADR for the high-HP inversion + root→sound swap (why heal/root
  punished the engage the kit demands).
- `gradle test` green, then `gradle deployClient` (never `compileJava`
  alone for client-visible work) and verify in game via
  `/class select <player>`: topped-off hits hard without taking hits;
  landing hits builds armor; `R` cleanses poison/slowness, speeds, horns;
  next hit builds +3; `G` at 0 stacks fizzles free; `G` at 10 stacks
  empowers the next swing (+16); an unswung 6s window expires with stacks
  spent; old `berserker` (and `warrior`) NBT picks load as Juggernaut.

## Acceptance

- Copy numbers match constants (no drift); manual checklist above passes.
- No behaviour-code changes beyond copy in this ticket.

## Comments

None yet.
