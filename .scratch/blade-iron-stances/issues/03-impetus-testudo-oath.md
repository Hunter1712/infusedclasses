# 03: Impetus/Testudo decisions, shared oath lane, window wiring (pure + apply)

Spec: `.scratch/blade-iron-stances/spec.md`

**Blocked by:** 02 (needs Legionary ids, stance, and runtime seams).

**Status:** done

## Task

The `G` payoff. One key, stance decides, shared cooldown:

- New `ImpetusAbility`: `NAME` "Impetus", `IMPETUS_BONUS` 3.0F,
  `IMPETUS_TAKEN_MULT` 1.25, `WINDOW_TICKS` 120 (6s), cast word `Ha!`, chat
  `Impetus!` red. Every melee hit inside the window earns the bonus, routed
  through `BonusCurve` (quad) like `Rend` — up to N swings x curved +3, so the
  20-HP TTK prices 2 full hits with a typical sidearm, not one next-hit spike.
- New `TestudoAbility`: `NAME` "Testudo", `TESTUDO_ARMOR` 8.0
  (`infusedclasses:testudo_plate`), `TESTUDO_DEAL_MULT` 0.7, `WINDOW_TICKS`
  120 (6s), no knockback while live, cast word `Hold!`, chat `Testudo!` gray.
  Instant fire, no charge gate.
- `Cooldowns`: one shared `OATH_MS` 50_000 lane (`useOath`/`oathRemaining`,
  `reset` readies it with the rest), read by both decisions. Both use the
  `trigger`/`fire` gate-and-arm shape (`FIRED`/`WRONG_CLASS`/`ON_COOLDOWN`);
  firing one arms the shared lane so the other reads on-cooldown for 50s.
  Window timestamps ride the transient runtime next to the lane; expiry is
  silent.
- Adapters: announce via the shared `AbilityApplier` seam, cast sound per
  ability on FIRED only (holders resolved via `javap` like the Rush/Rend
  sounds; fail silent if missing). Fizzles stay silent except the cooldown
  line; wrong-class stays fully silent. Milk changes nothing (timestamps, not
  effects); death/switch/logout clears both windows with everything else.

## Acceptance

- Pure JUnit: each fires only in its stance off cooldown; fire arms the shared
  lane (other reads ~50s); rejects arm nothing; window live/expiry; mults
  stack (Gladius+Impetus take x1.5, Scutum+Testudo deal x0.56); `Impetus`
  bonus at half charge reads quarter via `BonusCurve`.
- `gradle test` green. No copy/docs changes beyond string constants.

## Comments

Done: `ImpetusAbility`/`TestudoAbility` on shared 50s oath lane, windows as
runtime timestamps, `G` bind morphs by stance, announce + cast sound on FIRED via
shared seam, Testudo plate/brace through applier. `gradle test` green.
