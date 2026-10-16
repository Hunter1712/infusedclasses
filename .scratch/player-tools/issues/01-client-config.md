# 01: Client config file (hud.enabled, hud.corner)

Spec: `.scratch/player-tools/spec.md`

**What to build:** The one file a player can edit: `config/infusedclasses.json` holding exactly `hud.enabled` (default true) and `hud.corner` (default `bottom_right`, or `bottom_left`). Written with defaults on first run, read once at client startup, and never a crash: garbage, wrong types, unknown keys and unknown values fall back per field. Purely client-side; only the HUD reads it.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] First client run creates the file with the two documented fields and defaults.
- [ ] Parse tests: empty/garbage → defaults; both corner values parse; unknown corner → default; a wrong-typed field falls back alone, the other kept; unknown keys ignored; a written file round-trips.
- [ ] Editing `corner` and restarting moves the strip (together with status-hud 04); no other behaviour changes.
- [ ] `gradle test` green.

## Comments

None yet.
