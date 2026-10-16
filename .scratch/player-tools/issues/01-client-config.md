# 01: Client config file (hud.enabled, hud.corner)

Spec: `.scratch/player-tools/spec.md`

**What to build:** The one file a player can edit: `config/infusedclasses.json` holding exactly `hud.enabled` (default true) and `hud.corner` (default `bottom_right`, or `bottom_left`). Written with defaults on first run, read once at client startup, and never a crash: garbage, wrong types, unknown keys and unknown values fall back per field. Purely client-side; only the HUD reads it.

**Blocked by:** None (can start immediately).

**Status:** done

- [ ] First client run creates the file with the two documented fields and defaults.
- [x] Parse tests: empty/garbage → defaults; both corner values parse; unknown corner → default; a wrong-typed field falls back alone, the other kept; unknown keys ignored; a written file round-trips.
- [ ] Editing `corner` and restarting moves the strip (together with status-hud 04); no other behaviour changes.
- [x] `gradle test` green.

## Comments

Implemented on `wt-player-tools`.

- Landed: pure `client/HudConfig` (record `enabled`, `Corner` BOTTOM_RIGHT/BOTTOM_LEFT, string-in `parse` with per-field fallback, `toJson()` canonical text, `defaults()`), thin `client/HudConfigFile` (resolves `config/infusedclasses.json` from `FabricLoader`'s config dir, writes defaults when the file is missing, one warn on unreadable disk, never throws), and one load at client startup in `client/InfusedClassesClient.onInitializeClient` with `hudConfig()` exposed for the status HUD element.
- `HudConfigTest` (8 tests) pins every parse-tests box, including `toJson()` → `parse` round-trip.
- Deviation: invalid-but-readable content falls back silently per field; the single log line covers the unreadable-disk path only.
- Manual in-game pass pending: first-run file creation and the `corner`-edit-moving-the-strip check (the latter needs status-hud 04); no `gradle deployClient` in this lane.
- Full `gradle test`: 201 tests, 0 failures, 25 classes.
