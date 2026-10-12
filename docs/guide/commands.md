# Commands

Gamemaster-only `/class` wiring. All picks validate in the class choice module; an accepted switch clears what the old class earned (stacks, stance auras, cooldowns, pending windows) like the picker path does.

## `/class set <player> <class>`

Sets the player's class choice. Suggests registered playable ids only (no locked or unknown ids offered).

- One word, validated: unknown, missing or locked ids yield `Unknown or locked class` and leave the previous choice untouched.
- Accepted: writes the choice, clears old-class earnings, resets cooldowns.

## `/class select <player>`

Testing-only. Reopens the picker on the player's current page for GUI iteration — used after alpha updates when players are told to reselect, and by developers iterating on card copy.

- Carries the current class so reopening lands on its page.
- Picking still validates server-side; the picker closes only on the pick confirm. A rejected pick stays open for retry.

## Picker behaviour (no command needed)

- First join with no class choice opens the picker automatically.
- Rejoining with a saved choice loads silently — no GUI.
- `R` arrives as rotate intent for Legionaries and Rush for everyone else; `G` arrives as oath intent for Legionaries and Rend for everyone else (bind morph). Binds survive the class.
