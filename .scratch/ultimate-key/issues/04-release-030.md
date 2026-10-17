# 04: Release 0.3.0

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** The wave ships as one release: version 0.3.0, the changelog section covering all four specs (HUD added; countdowns and keys changed; roulette re-cut), the README jar example updated, and one end-to-end pass on the live instance collecting every checklist (status-hud 04, player-tools 02–04, roulette 02, ultimate-key 01). Nothing persisted changes shape, so no wipe guidance.

**Blocked by:** all fourteen tickets above — run last: `.scratch/status-hud` 01–05, `.scratch/player-tools` 01–04, `.scratch/roulette-no-heal` 01–02, and 01–03 of this folder.

**Status:** ready-for-agent

- [x] `gradle test` and `gradle build` green; the built jar carries `0.3.0`.
- [x] `gradle.properties` and README updated; the live `mods/` jar refreshed via `gradle deployClient`.
- [x] CHANGELOG `0.3.0` section dated, covering Added/Changed/Fixed per the spec.
- [ ] Every checklist above done in one sitting.

## Comments

Landed: `mod_version=0.3.0`, README jar example, dated `0.3.0` CHANGELOG section (Added/Changed across all four specs). `gradle build` green; the jar carries 0.3.0. The live-jar deploy and the one-sitting end-to-end pass inherit the open manual checks from status-hud 04, player-tools 01-04 and roulette 02 (deployClient runs last so the live jar carries the full reviewed wave, not an interim one).
Review round done: review fixes landed (glossary `Class command` entry, Rend-pending flag read, config log-once), then `gradle deployClient` shipped the reviewed jar; the superseded `infusedclasses-0.2.0.jar` was removed from the live `mods/` folder so only 0.3.0 loads. Open: the one-sitting in-game pass over every manual checklist (Controls, strip, lights, config, info/pick, spins, emission).
