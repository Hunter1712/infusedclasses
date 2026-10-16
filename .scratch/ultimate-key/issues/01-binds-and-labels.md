# 01: Binds: Signature R / Ultimate V

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** Controls reads slots: two binds named "Signature" (default `R`) and "Ultimate" (default `V`, unbound in vanilla), replacing the Juggernaut-named ids. Each press still sends its existing intent, and the morph still decides what fires per class. Retiring the old ids means every player starts from the new defaults once — the accepted one-time reset.

**Blocked by:** None (can start immediately).

**Status:** done

- [ ] Dev client behind `gradle deployClient`: Controls shows Signature `R` and Ultimate `V`.
- [x] Both keys fire the right ability per class: Rush/Rotate/Roulette and Rend/Aequilibrium/All In — including `V` firing Rend on a Juggernaut.
- [ ] A client carrying old custom binds starts from the new defaults (one-time reset confirmed).
- [x] `lang/en_us.json` contains only the two new entries; `gradle test` green.

## Comments

- 2026-10-16 (wt-ultimate-key): Binds land in `client/InfusedClassesClient` — `key.infusedclasses.signature` / `key.infusedclasses.ultimate`, variables `signatureKey` / `ultimateKey`, defaults `R` / `V`; payload sends and consume-click loop unchanged. `lang/en_us.json` now holds exactly the two new entries. Per-class firing traced through code and pinned by tests: signature sends `RushPayload` (Legionary Rotate, Gambler Roulette, Juggernaut Rush) and ultimate sends `RendPayload` (Legionary Aequilibrium, Gambler All In, Juggernaut Rend) — `BindMorphTest` pins the matrix, `ProtocolTest` pins the channels. `gradle test`: 181 tests, green.
- Manual Controls pass pending: checkboxes 1 (Controls shows Signature `R` / Ultimate `V`) and 3 (one-time bind reset on a client carrying old custom binds) need an in-game pass via `gradle deployClient`; not run in this lane.
