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
It opens once, on first join with no saved class choice. Rejoining with a choice loads silently. Admins can reopen it with `/class select <player>`. A rejected pick stays open until the server confirms — check the log for `Unknown or locked class`.

**My choice didn't stick / wrong class after update.**
Alpha jars may invalidate choices. Reselect in the picker or ask an admin for `/class set <player> <class>`.

## Gameplay

**My stacks vanished after relog / respawn / class switch.**
By design. Stacks, stance windows, cooldown lanes and pending bonuses are transient — only the class choice is saved. See [stances-passives.md](stances-passives.md).

**Spam-clicking earns nothing.**
By design. The stack gate feeds conditional triggers only at full charge, and hook bonuses scale quadratically with charge — half charge pays a quarter, spam pays chips. Time your swings.

**Rend did nothing / said `No Momentum`.**
Rend needs at least one Momentum stack and an off-cooldown lane (60s). Firing spends every stack plus the armor up front, and the bonus lands on the next melee hit within 6s — an unswung window expires with the stacks already spent.

**Rush did nothing.**
Rush has its own 25s lane. Rejected presses (wrong class, on cooldown) leave the lane, window and bonus untouched — wait out the cooldown and press again.

**Rotate did nothing.**
Rotate has an 8s lock. Rejects arm nothing and grant no buff — flip when the lock is ready.

**R and V do different things per class?**
Yes. `R` is the Signature bind — Rush for Juggernaut, Rotate for Legionary, Roulette for Gambler; `V` is the Ultimate bind — Rend for Juggernaut, the Aequilibrium oath for Legionary, All In for Gambler. Binds survive the class by design. Keys are remappable: Controls is the truth for what a slot is bound to, and `/class info` names the slots' abilities.

## Reporting problems

Single funnel: GitHub Issues. Include the jar name, MC / loader / Fabric API versions, the `logs/latest.log` crash section (not screenshots), and repro steps (class, stance/passive, server or client). Wrong-loader or wrong-version reports without a log close as wontfix.
