# 04: Warrior registry and picker copy

**What to build:** Warrior carries the Berserk pair and the picker describes them honestly.

**Blocked by:** 01.

**Status:** done

- [x] Warrior `ClassDefinition` gains both passives (damage + speed, same 10%-bucket curve) without changing Bulwark or Second Wind.
- [x] Picker copy generated from the numbers (per-step and max, e.g. 10% steps to +4.0 / +0.8); card body shows names only, tooltip shows full form.
- [x] Locked classes unchanged (`Passives: ???`, `COMING SOON`).
- [x] Presentation tests assert card lines plus tooltip copy matching the numbers exactly.

## Comments

Pair lives as `List<HpScaledPassive> hpPassives` (separate list: no trigger/stacks/grace to
lie about). Card body appends both names under the same header; tooltip uses the same
`Passive: name — copy` shape. Shipped via `deployClient`.
