# Spec: Player-facing `/class info`, `/class pick`, and the client config file

Status: ready-for-agent

## Problem Statement

The `/class` tree is gamemaster-only from its root, so a player cannot ask what class they have or when their lanes come back — the picker card is the only copy they can read, and chat speaks only when an ability fires. Picking is likewise op-only today (`/class select <player>` is a testing tool for someone else's screen), so an op iterating on their own GUI has no self-serve way in. And with a status HUD arriving there is now exactly one thing a player may want to configure — its corner — with no config file to hold it: `docs/guide/configuration.md` still promises a file that does not exist.

## Solution

- **Bare `/class` and `/class info` are open to everyone and speak for the invoking player**: the class's display name plus one line per lane it carries, countdowns in one decimal (`Rush: 18.4s`, `Rend: ready`), and the Legionary's current stance. No class → a single line saying so.
- **`/class pick` reopens the picker for yourself**, gated on having cheats (permission level ≥ gamemaster — vanilla's per-player meaning of "has cheats"). Without cheats the command is not in the command tree at all: vanilla only syncs nodes whose requirements pass, so there is nothing to reject and no rejection copy to write. It lands on the current class's page, like the select path.
- **`set` and `select` keep their gamemaster gates**, moved from the root onto the subcommands; `select` stays testing-only. A plain player's tree therefore shows exactly `class` and `info`.
- **A client config file lands**: `config/infusedclasses.json` with exactly `hud.enabled` (bool, default true) and `hud.corner` (`bottom_right` default, `bottom_left` optional). Purely client-side, read once at client startup; missing or invalid falls back to defaults (the file is written with defaults on first run); unknown keys and bad values are ignored, never a crash. No tuning numbers, no other knobs — `reselect.enabled` was dropped: reselection is the picker's own flow.

## User Stories

1. As a player, I want `/class info` to tell me my class and each lane's remaining time, so I can plan presses even without the HUD.
2. As a player with no class, I want `/class` to say so plainly.
3. As a Legionary, I want my current stance in the info lines, so I can check the flip without looking.
4. As an op, I want `/class pick` to reopen my own picker on my current page, so GUI iteration needs nobody else.
5. As a player without cheats, I want `pick` hidden rather than promised and rejected.
6. As any player, I want bare `/class` to equal `/class info`, so I never have to remember the subcommand.
7. As an admin, I want `set` and `select` unchanged and still gated, so opening the root cannot expose admin verbs.
8. As a modpack user, I want a documented config file at a documented location with documented defaults.
9. As a player with a hand-edited or broken config, I want the mod to boot anyway and use defaults.
10. As a tester, I want the info lines and the config parse asserted purely, so regressions show without a running client.

## Implementation Decisions

- `ClassCommands.register()`: the root literal drops its `.requires`; the root itself `executes` the info path; `set`, `select` and the new `pick` each carry `.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))` on their own literal. `select` keeps its testing-only comment and behaviour.
- `/class info`: player-only (console gets vanilla's usual player-required failure); builds lines from `PlayerClassAccess.of(player).currentDefinition()` plus `InfusedClasses.runtime(player)`. Lines: `Class: <displayName>` (gray, matching the pick line's tone) then one line per lane the class carries — `Rush: 18.4s` / `Rend: ready` — reusing `ChatLine.cooldown` for cooling lanes (which is now `%.1fs`); Legionary adds `Stance: <name>`. No class → `No class chosen` (gray), nothing else.
- The info line composition is a new pure decision `ClassInfo.lines(def, runtime, now)` returning the lines for the entry to send; the lane mapping (ability → remaining read) is explicit beside it until a second caller needs it. The decision reads only the definition, the runtime, and the passed `now`.
- `/class pick`: self-only, cheats-gated as above; on run it sends `OpenClassScreenPayload` with the player's current class id (empty when none), exactly like `select` does for its target — no chat output, the picker is the feedback. Nothing else about the picker flow changes (rejected picks stay open for retry, closing still happens only on the confirm).
- Cheats semantics recorded once: permission level ≥ `Permissions.COMMANDS_GAMEMASTER` (level 2) is vanilla's per-player "has cheats" — ops, the singleplayer owner with cheats, LAN-allow-commands. The literal `allowCommands` server flag was rejected (hardcoded false on fresh dedicated worlds; it is not what "cheats on" means per player).
- New pure `HudConfig` (record: `enabled`, `Corner` enum `BOTTOM_RIGHT`/`BOTTOM_LEFT`) plus a string-in `parse` half and a thin `HudConfigFile` adapter: load from `FabricLoader.getInstance().getConfigDir().resolve("infusedclasses.json")` at client init, write defaults when the file is missing, fall back to defaults on unreadable/invalid content (log once). Field names in the file are `hud.enabled` and `hud.corner` with `bottom_right`/`bottom_left` values; unknown keys and values are ignored. The HUD element (`.scratch/status-hud` issue 04) is the only consumer.
- Docs: `docs/guide/configuration.md` rewritten from the promise to the real file (location, full field table with defaults, backfill/validation rules, client-side-only and restart-to-apply notes); `docs/guide/commands.md` gains the open root, `info` and `pick` sections and a gates table.

## Testing Decisions

- New `ClassInfoTest`: lines for each class at ready/cooling boundaries with one-decimal pins (`18.4s`), the no-class line, the stance line present for Legionary only, unknown/missing class safe, and no line carrying a heal or key reference the class does not have.
- New `HudConfigTest`: parse defaults on empty/garbage input; `bottom_left` and `bottom_right` parse; unknown corner value → default; wrong types → field default; unknown keys ignored; a written file round-trips through parse.
- Not unit-tested (house convention, manual dev pass via `gradle deployClient`): the command wiring itself — the hidden tree for non-cheats, the gates on `set`/`select`, `pick` sending the payload — and the file IO half.
- `gradle test` green.

## Out of Scope

- `/class info <player>` or any target args; richer theming beyond the gray/plain tones; info showing passives or stack counts (that is the HUD's job).
- Server-side config; a config screen or reload command; any knob besides `hud.enabled`/`hud.corner`.
- Reopening the picker for players without cheats (join-time choosing and `/class set` cover the supported flows).

## Further Notes

- Why `pick` is cheats-gated: the requirement was "should only work with cheats on", and cheats means permission ≥ gamemaster per player — not the server profile's `allowCommands` flag.
- The root becomes open for the first time; the one risk is a gate left behind on the root or missing on a subcommand. The manual pass checks the tree as a non-op and an op.
- `reselect.enabled` was dropped from the earlier config sketch: alpha reselection flows through the picker, and a knob for it never found a caller.
- The config file is new state on disk for the client; none of it is read by the server and none of it affects play beyond drawing.
