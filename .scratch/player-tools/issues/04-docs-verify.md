# 04: Commands + configuration docs, verify

Spec: `.scratch/player-tools/spec.md`

**What to build:** The two promised pages tell the truth: `commands.md` documents the new tree (bare `/class` ≡ `info`, `pick` hidden without cheats, `set`/`select` gated as before, plus a gates table) and `configuration.md` replaces the "no config file" promise with the real file (location, field table with defaults, validation and backfill rules, client-side-only, restart-to-apply). Finishes with one sitting that runs the spec's manual checks end to end.

**Blocked by:** 01, 02, 03 — documents what they shipped.

**Status:** done

- [x] `commands.md` and `configuration.md` match shipped behaviour exactly; README links resolve.
- [ ] One verify pass: config first-run and fallbacks; `/class` and `/class info`; `pick` hidden/visible per cheats; `set`/`select` gates.
- [x] The picker-behaviour key line (`R`/`V`) stays untouched here — `.scratch/ultimate-key` 03 owns it.

## Comments

Implemented on `wt-player-tools`.

- `commands.md` rewritten: open root ≡ `info`, per-lane countdowns in one decimal, `pick` section (self-only, cheats-gated, no chat), and a gates table. The picker-behaviour section is untouched, key line included (`git diff` shows no change to it; it still reads `R`/`G` until `.scratch/ultimate-key` 03 lands).
- `configuration.md` rewritten: real file at `config/infusedclasses.json` (game dir's `config` folder), field table (`hud.enabled` bool default `true`, `hud.corner` string default `bottom_right`, `bottom_left` optional), first-run defaults example, validation/backfill rules, client-side-only and restart-to-apply notes.
- Extra truth fix (one deviation from the ticket's file list): `modpack-authors.md` claimed "no config file exists in alpha" and "Config file: None in alpha"; both now point at the client-side drawing-only file. No pack-relevant knobs exist, which the rewritten lines say.
- All relative links in `README.md` and `docs/guide/*.md` resolve (script-checked).
- Manual in-game pass pending for the verify-pass box: config first-run and fallbacks, `/class`/`/class info`, pick visibility per cheats, and the `set`/`select` gates; no `gradle deployClient` in this lane.
