# 01: War Cry decision, gating list, and cooldown lane

**What to build:** Pure shout decision plus the multi-ability gating it needs and its own cooldown lane.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] `WarCryAbility` with `NAME`, `RADIUS_BLOCKS = 16.0`, `EFFECT_TICKS = 160`, Weakness/Slowness ids at amplifier 0; kinds FIRED/WRONG_CLASS/ON_COOLDOWN; `trigger` read-only, `fire` gates and arms together; `War Cry!` red announcement.
- [x] `ClassDefinition.activeAbility` becomes `activeAbilities: List<Identifier>`; both ability decisions scan it; locked rejects non-empty, playable requires non-empty; existing constructors updated.
- [x] `Cooldowns` gains `WAR_CRY_MS = 60_000L` with `useWarCry`/`warCryRemaining`; `reset` readies both; `PlayerRuntime` exposes `tryUseWarCry`/`warCryRemaining`.
- [x] JUnit: gates, fire arms only its lane, rejects arm nothing, constants exact, lanes independent, reset readies both.

## Comments

Shipped plus one deviation: cooldowns regrew as two plain fields, not the deleted inner `Lane` class — less code for two lanes. `WarCryAbilityTest`: 6 tests green.
