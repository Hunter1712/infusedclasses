# 01: Binds: Signature R / Ultimate V

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** Controls reads slots: two binds named "Signature" (default `R`) and "Ultimate" (default `V`, unbound in vanilla), replacing the Juggernaut-named ids. Each press still sends its existing intent, and the morph still decides what fires per class. Retiring the old ids means every player starts from the new defaults once — the accepted one-time reset.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Dev client behind `gradle deployClient`: Controls shows Signature `R` and Ultimate `V`.
- [ ] Both keys fire the right ability per class: Rush/Rotate/Roulette and Rend/Aequilibrium/All In — including `V` firing Rend on a Juggernaut.
- [ ] A client carrying old custom binds starts from the new defaults (one-time reset confirmed).
- [ ] `lang/en_us.json` contains only the two new entries; `gradle test` green.

## Comments

None yet.
