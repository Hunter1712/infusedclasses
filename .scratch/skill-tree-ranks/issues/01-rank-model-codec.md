# 01: Rank model, codec migration, slower XP

**What to build:** Rank-map progress (`id -> rank`) replacing the unlocked set, the rank-suffixed save codec with legacy folding (bare ids count as rank one, old vigor/plate/ward/hide ids fold into Vigor/Bulwark/Hide capped at five, removed and unknown ids keep dropping), the experience base cost raised by roughly half with the exponential-through-20 plus flat-ceiling shape unchanged, and the Defence node data (Vigor 0/5, Bulwark 0/5 alternating, Hide 0/5, Thick Skin, Hold the Line, Enduring ring, Rallying Roar).

**Blocked by:** None (pure data layer, testable standalone).

**Status:** ready-for-agent

- [x] Ranks accrue to max and reject beyond; respec refunds rank-times-cost
- [x] Legacy saves fold into ranks with the Bulwark cap; unknown ids drop; high-level round-trips stay uncapped
- [x] Level 1-20 curve keeps its shape, post-20 stays flat, full suite green

## Comments

CLOSED — implemented in `3c68c71`, full suite green (`gradle test --rerun-tasks`). No live-client check needed: pure data layer.
