# FAQ & troubleshooting

## Install / launch

**Do I need Fabric API?**
Yes. Install Fabric API for 1.21.11 alongside Fabric Loader 0.19.5+ and Java 21.

**The game says I'm missing a dependency.**
Usually a missing or outdated **Fabric API**, the **wrong jar** for your game version, or the jar installed on **only one side** — the supported setup installs the same jar on both client and server. See [installation.md](installation.md).

**Does it work with vanilla clients?**
No. Install the mod on both client and server. All logic is server-side and server-authoritative; the client sends intents and renders the picker. See [compatibility.md](compatibility.md#client--server).

**Will it break my existing world?**
Saves load fine — new players pick on next join. But 0.x jars may break worlds between updates with no migration until beta. Expect wipes and class resets at any time; reselect with `/class select <player>` when told to.

## Picker

**The picker never opened.**
It opens once, on first join with no saved class choice. Rejoining with a choice loads silently. Admins can reopen it with `/class select <player>`. A rejected pick stays open until the server confirms — the player sees a red `Unknown or locked class` line in chat.

**My choice didn't stick / wrong class after update.**
Alpha jars may invalidate choices. Reselect in the picker or ask an admin for `/class set <player> <class>`.

## Gameplay

**My stacks vanished after relog / respawn / class switch.**
By design. Stacks, stance windows, cooldown lanes and pending bonuses are transient — only the class choice is saved. See [stances-passives.md](stances-passives.md).

**Spam-clicking earns nothing.**
By design. The stack gate feeds conditional triggers only at full charge — half swings and spam
earn zero Momentum and roll no crit. Time your swings.

**Onslaught did nothing / said `No Momentum`.**
Onslaught needs at least 10 Momentum and an off-cooldown lane (60s). Inside its 15s window each
full-swing hit deals +1 damage and spends 2 stacks; nothing feeds the bar until the window ends.
If the bar empties first, the window ends there.

**Offload did nothing.**
Offload needs at least 5 Momentum and its own 10s lane. A press below the price says
`No Momentum`; a wrong class or a press on cooldown rejects silently. The stacks are spent at
the press, so an unspent window owes nothing back.

**Rotate did nothing.**
Rotate has an 8s lock. Rejects arm nothing and grant no buff — flip when the lock is ready.

**R and V do different things per class?**
Yes. `R` is the Signature bind — Offload for Juggernaut, Rotate for Legionary, Roulette for Gambler; `V` is the Ultimate bind — Onslaught for Juggernaut, the Aequilibrium oath for Legionary, All In for Gambler. Binds survive the class by design. Keys are remappable: Controls is the truth for what a slot is bound to, and `/class info` names the slots' abilities.

## Reporting problems

Single funnel: GitHub Issues. Include the jar name, MC / loader / Fabric API versions, the `logs/latest.log` crash section (not screenshots), and repro steps (class, stance/passive, server or client). Wrong-loader or wrong-version reports without a log close as wontfix.
