# 04: Forced shout particles

**What to build:** Make the victim puffs setting-proof. Playtest showed `Particles: Minimal`
hides them entirely — the code fires, the client drops them.

**Blocked by:** None (adapter-only).

**Status:** done

- [x] Victim puffs switch to the forced `sendParticles` overload (`force=true` skips the particle limiter, `longDistance=true` skips distance culling); type and positions unchanged.
- [x] Count 6 → 12 for presence; volume/pitch of the roar untouched.
- [x] Heal crit puff deliberately untouched (shout scope only).
- [x] JUnit cannot cover pixels; manual check on Minimal plus a ~30-block observer.

## Comments

Shipped. Convention going forward: every new `sendParticles` call uses the forced overload or it ships Minimal-blind.
