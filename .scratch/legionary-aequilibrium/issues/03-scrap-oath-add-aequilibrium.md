# 03: Move oath lane to Aequilibrium, add ultimate

Spec: `.scratch/legionary-aequilibrium/spec.md`

**Blocked by:** 01 (reuses the window + lane pattern).

**Status:** done

## Task

One clean ultimate on `G`. The oath lane keeps its verbs, changes owner:

- New `AequilibriumAbility`: `NAME` "Aequilibrium", `EDGE_BONUS` 1.0
  (`infusedclasses:aequilibrium_edge`), `PLATE_BONUS` 3.0
  (`infusedclasses:aequilibrium_plate`), `WINDOW_TICKS` 120 (6s),
  announcement `Aequilibrium!` gold. `trigger`/`fire` in the gate-and-arm
  shape on the reused oath lane (`Cooldowns.OATH_MS` 50_000,
  `useOath`/`oathRemaining` verbs unchanged). Fires in either stance;
  wrong-class only for non-Legionaries. Rejects arm nothing.
- `ClassRegistry`: add `AEQUILIBRIUM_ABILITY`
  (`infusedclasses:aequilibrium`). `LEGIONARY` actives become
  `[ROTATE_ABILITY, IMPETUS_ABILITY, TESTUDO_ABILITY, AEQUILIBRIUM_ABILITY]`
  with all four descs built from constants (Impetus/Testudo descs rewritten
  as flip buffs in ticket 01). Impetus/Testudo ids stay.
- `BindMorph.OathTarget`: drop `IMPETUS`/`TESTUDO`, add `AEQUILIBRIUM`;
  `oathFor` returns it for any Legionary stance (morph deleted). Follow the
  `InfusedClasses` oath dispatch through the enum rename.
- Window aura (`edge` + `plate`) rides `StanceApplier` while live,
  write-only-on-change, single aura list for write/remove. Overlay: stance
  stays underneath, both layers count.
- Picker copy for the new active built from constants; card shows all four
  actives.

## Acceptance

- Pure JUnit: Aequilibrium fires in either stance, rejects arm nothing;
  shared 50s lane arms on fire; window live/expiry at 6s; overlay keeps
  stance deal/take mults; no oath-window text remains in copy.
- `gradle test` green.

## Comments

Landed: `AequilibriumAbility` on the reused oath lane, registry carries four actives,
`OathTarget` is `REND`/`AEQUILIBRIUM`, entry dispatches two-way, `StanceApplier` rides
the new window aura. Cast sound `entity.player.levelup` (spec named none).
