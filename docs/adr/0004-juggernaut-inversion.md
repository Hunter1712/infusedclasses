# ADR-0004: Juggernaut inversion (high-HP rewards) and root-to-sound swap

Status: accepted

## Context

The Berserker kit (ADR lineage: comeback brawler) demanded getting hurt while
topped-off: `Bulwark` needed `on_hit_taken`, `Fury` needed missing HP, Bloodlust
fizzled above ~5 hearts and healed the Fury bucket that powered it, and both
actives rooted the caster 1s via `Spellcast`. Cautious players — the majority in
Minecraft — got zero passives and two fizzles, and the optimal loop was a
`R -> G` autopilot that never required tanking.

## Decision

1. **Invert the engine.** `Fury` (missing HP) becomes `Vigor` (current HP, same
   per-steps); `Bulwark` (hit-taken, 20x0.5) becomes `Momentum` (hit-dealt,
   10x1.0 armor, 4s grace). Dive topped, build by hitting, spend before bleeding
   out. Heals now help instead of cooling.
2. **Engage plus empower.** Rush (`R`, 25s): Speed I 4s, cleanse all harmful
   effects, next melee hit +3 Momentum. Rend (`G`, 60s): empowers the next melee
   hit within 6s for `6 + 1/stack` bonus damage, consuming all Momentum up front.
   One mobility tool plus one builder/spender instead of two redundant recoveries.
   (Amended: Rend was a direct execute with a sub-half-HP multiplier; it is now a
   next-hit empower with no target-HP check, so `G` needs no target in range.)
3. **Sound replaces root.** `Spellcast` self-slow is deleted; every cast plays
   its own sound on FIRED only (wind burst for Rush, anvil land for Rend). Rooting a melee
   engage punished the exact behaviour the kit demands. (Amended: horn/roar as first
   specified; swapped in 89eccfd.)
4. **Full rename.** Berserker becomes Juggernaut (`infusedclasses:juggernaut`);
   stored `berserker` (and legacy `warrior`) ids map forward on load, no wipes.

## Consequences

- Kit functions for kiters: Rush always fires, Rend needs landed hits, not taken
  ones. Failed dives still punish (low HP means no Vigor, no sustain).
- `ServerEvents` no longer feeds `on_hit_taken`; the attacker branch feeds
  `on_hit_dealt` plus the one-shot Rush bonus, then spends a live Rend empower
  snapshot as bonus damage through the adapter. The extra hurt re-enters the hook
  on the same thread, guarded by a flag in the adapter so it earns nothing twice.
- Cleanse scope is category-based (`HARMFUL`), so future harmful effects are
  covered without code changes.
