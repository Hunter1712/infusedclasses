# 04: Release 0.3.0

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** The wave ships as one release: version 0.3.0, the changelog section covering all four specs (HUD added; countdowns and keys changed; roulette re-cut), the README jar example updated, and one end-to-end pass on the live instance collecting every checklist (status-hud 04, player-tools 02–04, roulette 02, ultimate-key 01). Nothing persisted changes shape, so no wipe guidance.

**Blocked by:** all fourteen tickets above — run last: `.scratch/status-hud` 01–05, `.scratch/player-tools` 01–04, `.scratch/roulette-no-heal` 01–02, and 01–03 of this folder.

**Status:** ready-for-agent

- [ ] `gradle test` and `gradle build` green; the built jar carries `0.3.0`.
- [ ] `gradle.properties` and README updated; the live `mods/` jar refreshed via `gradle deployClient`.
- [ ] CHANGELOG `0.3.0` section dated, covering Added/Changed/Fixed per the spec.
- [ ] Every checklist above done in one sitting.

## Comments

None yet.
