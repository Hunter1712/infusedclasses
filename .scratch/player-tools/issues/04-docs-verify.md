# 04: Commands + configuration docs, verify

Spec: `.scratch/player-tools/spec.md`

**What to build:** The two promised pages tell the truth: `commands.md` documents the new tree (bare `/class` ≡ `info`, `pick` hidden without cheats, `set`/`select` gated as before, plus a gates table) and `configuration.md` replaces the "no config file" promise with the real file (location, field table with defaults, validation and backfill rules, client-side-only, restart-to-apply). Finishes with one sitting that runs the spec's manual checks end to end.

**Blocked by:** 01, 02, 03 — documents what they shipped.

**Status:** ready-for-agent

- [ ] `commands.md` and `configuration.md` match shipped behaviour exactly; README links resolve.
- [ ] One verify pass: config first-run and fallbacks; `/class` and `/class info`; `pick` hidden/visible per cheats; `set`/`select` gates.
- [ ] The picker-behaviour key line (`R`/`V`) stays untouched here — `.scratch/ultimate-key` 03 owns it.

## Comments

None yet.
