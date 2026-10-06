# 02 — War Cry builder grant

Status: done

Spec: `.scratch/warrior-balance/spec.md`

Blocked by: 01

## Task

Make War Cry a builder: firing (R) credits 5 Bulwark stacks on top of the existing
debuff (Weakness I + Slowness I, 10s, 16 blocks).

- Decide-then-apply split: the grant is resolved in the applier, not the entry —
  mirror the `consumeSpent` shape (`PassiveApplier.java:106`).
- Cap-respecting: `min(maxStacks, stacks + 5)` via the existing `onTrigger`-style
  path (`PlayerRuntime.java:66-76`), re-stamps grace.
- Root kept (`Spellcast` untouched). Radius/amplifier unchanged. Fizzle/reject
  paths unchanged (wrong class silent, on-cooldown reports, grants nothing).

## Acceptance

- Pure JUnit: fire at 0 stacks → 5; at 18 → capped 20; on-cooldown/wrong-class
  → no grant, lane untouched.
- Manual: `/class select`, press R in a pack — debuff lands, armor ticks up.
- `gradle test` green.

## Comments

None yet.
