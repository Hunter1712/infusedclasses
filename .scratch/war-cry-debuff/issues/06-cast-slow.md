# 06: Shared cast slow

**What to build:** Every active roots its caster briefly — one learnable rule. Short heavy
Slowness, identical on War Cry and Second Wind, disclosed in picker copy.

**Blocked by:** None.

**Status:** done

- [x] `Spellcast` constants: Slowness id, 40 ticks, amplifier 2; shared, never per-ability.
- [x] One shared applier helper (not duplicated); both adapters call it on FIRED only — fizzles and rejects cost nothing.
- [x] Both picker descriptions gain a "roots you" clause built from the constants.
- [x] Tooltip test pins copy to numbers; in-game feel (brief root icon, escape still possible) is manual.

## Comments

Shipped. Second Wind fizzle applies no slow (FIRED-gated); War Cry slow lands with the debuffs. 95 green, jar deployed.

Follow-up: tuned to 20 ticks (1s) per playtest — copy and tooltip follow the constant.
