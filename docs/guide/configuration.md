# Configuration

One file, client-side only: `config/infusedclasses.json` — the `config` folder inside your instance's game directory, next to `mods` and `saves`. It holds the status HUD's two knobs and nothing else. The server never reads it and it changes nothing about play beyond drawing.

It is read once when the client starts, so edit the file and restart the game to apply a change. There is no reload command and no in-game config screen.

## Fields

| Field | Type | Default | Values | What it does |
|-------|------|---------|--------|--------------|
| `hud.enabled` | boolean | `true` | `true` / `false` | `false` stops the status HUD drawing, and nothing else |
| `hud.corner` | string | `left` | `left`, `right` | the screen zone the status HUD stacks into: flush to that screen edge, vertically centered (`left` default; `right` mirrors it) |

On first run the file is written with both defaults:

```json
{
  "hud": {
    "enabled": true,
    "corner": "left"
  }
}
```

## Validation and backfill

- Missing file: written with the defaults above, with no restart needed for them to apply.
- Garbage or non-object content: the defaults are used and the client boots anyway.
- Per-field fallback: a wrongly typed or unknown value defaults that field alone and keeps the other. An unknown `corner` (say `top`) does not stop anything.
- Unknown keys are ignored. There are no hidden knobs: the two fields above are the whole file.
- No migration: a field missing from an older file is simply defaulted.
