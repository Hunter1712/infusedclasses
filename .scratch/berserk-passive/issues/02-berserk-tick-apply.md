# 02: Berserk tick apply

**What to build:** Recompute both Berserk modifiers every server tick from live HP and make attributes agree, writing only on bucket change.

**Blocked by:** 01.

**Status:** done

- [x] Tick path reads live HP (current/max), runs the 01 decision for both attributes.
- [x] `PassiveApplier` writes only when applied amount differs; same bucket twice costs no write.
- [x] Bucket 0 removes the modifier (no stale bonus at full HP); heals shrinking the bucket write down automatically.
- [x] Bulwark tick path (`tickDecayMoved`, grace, stacks) unchanged.

## Comments

Landed in `PassiveApplier.apply`: one bucket from live HP, then per-entry write/remove
reusing the existing early-out. No new tick plumbing; runtime untouched.
