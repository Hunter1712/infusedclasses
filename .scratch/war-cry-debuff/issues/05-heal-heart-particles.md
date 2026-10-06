# 05: Heart puff for Second Wind

**What to build:** Replace the heal's CRIT puff (reads as hitting, not healing, and Minimal
hides it) with forced HEART particles.

**Blocked by:** None (adapter-only).

**Status:** done

- [x] `AbilityApplier` heal visual becomes HEART, ~12 count above the player, on the forced `sendParticles` overload (same Minimal-proofing as 04).
- [x] Crit puff gone, not kept alongside — one visual per cast.
- [x] No decision, copy, or protocol change; existing heal tests stay green (adapter-only).
- [x] Manual check on Minimal: red hearts on every Second Wind fire.

## Comments

Shipped as `heartPuff`, replacing `critPuff` outright.
