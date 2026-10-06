# 03: Berserk lifecycle and spender isolation

**What to build:** Berserk stays transient and Second Wind never touches it.

**Blocked by:** 02.

**Status:** done

- [x] `onClassChanged` removes both Berserk modifiers on switch (nothing to clear besides the modifiers).
- [x] Respawn/logout leaves no stale Berserk modifier (existing transient clear path covers it).
- [x] Nothing persists to NBT.
- [x] `consumeSpent` / Second Wind resolves Bulwark by trigger and leaves both Berserk modifiers in place; zero-stack fizzle still arms nothing.
- [x] JUnit covers switch-removal for both modifier ids and consume-leaves-Berserk.

## Comments

`onClassChanged` mirrors the existing removal loop for `hpPassives`. Adapter-level removal
needs a live player, so the JUnit pin is at the decision seam: `bulwarkOf` still resolves
Bulwark with the pair present, and hits still earn Bulwark stacks alone — the spender can
never name a Berserk modifier. Modifier ids asserted in `BerserkTest`.
