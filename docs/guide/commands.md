# Commands

`/class` is the whole command surface. The root and `info` are open to every player; `set`, `select` and `pick` are gamemaster-gated. "Gamemaster" is permission level 2 — the per-player meaning of "has cheats": ops, the singleplayer owner with cheats on, LAN-allow-commands. All picks validate in the class choice module; an accepted switch clears what the old class earned (stacks, stance auras, cooldowns, pending windows) like the picker path does.

## `/class` and `/class info`

Open to everyone. Speaks for the invoking player only:

- `Class: <displayName>` first.
- One line per lane the class carries, with the remaining time in one decimal: `Offload: 3.4s`, `Onslaught: ready`. A lane off cooldown says `ready`.
- Legionaries also get `Stance: Gladius` or `Stance: Scutum`.
- No class yet: a single `No class chosen` line.

Console gets vanilla's usual player-required failure. There is no `/class info <player>`.

## `/class set <player> <class>`

Gamemaster only. Sets the player's class choice. Suggests registered playable ids only (no unknown ids offered).

- One id, validated: an id that matches no playable class — or is locked — yields `Unknown or locked class` and leaves the previous choice untouched. Text that is not a valid id fails at parse time with the vanilla argument error.
- Type the full id (tab-complete writes it): a bare name resolves to the vanilla `minecraft:` namespace, so `juggernaut` means `minecraft:juggernaut` and is rejected.
- Accepted: writes the choice, clears old-class earnings, resets cooldowns.

## `/class select <player>`

Gamemaster only, testing-only. Reopens the picker on the player's current page for GUI iteration — used after updates when players are told to reselect, and by developers iterating on card copy.

- Carries the current class so reopening lands on its page.
- Picking still validates server-side; the picker closes only on the pick confirm. A rejected pick stays open for retry.

## `/class pick`

Gamemaster only — the per-player "has cheats". Reopens your own picker on your current class's page.

- Self only: no player argument.
- No chat output. The picker is the feedback; a rejected pick stays open for retry and the screen closes only on the pick confirm.
- Without cheats the command is not in your command tree at all: vanilla only offers nodes whose requirements pass, so it is never promised and then refused.

## The gates at a glance

| Command | Who can use it | What it does |
|---------|----------------|--------------|
| `/class`, `/class info` | everyone | your class, lane countdowns, Legionary stance |
| `/class set <player> <class>` | gamemaster | sets a player's class choice |
| `/class select <player>` | gamemaster | reopens a player's picker (testing tool) |
| `/class pick` | gamemaster | reopens your own picker |

## Picker behaviour (no command needed)

- First join with no class choice opens the picker automatically.
- Rejoining with a saved choice loads silently — no GUI.
- `R` arrives as the rotate intent for Legionaries, the roulette intent for Gamblers and Offload for everyone else; `V` arrives as the oath intent for Legionaries, All In for Gamblers and Onslaught for everyone else (bind morph). Binds survive the class.
