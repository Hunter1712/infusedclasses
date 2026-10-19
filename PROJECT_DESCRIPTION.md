# Infused Classes

> **Character classes for Minecraft (Fabric): Juggernaut bruiser, Legionary stance trader plus Gambler dice roller — BETA.**

**Infused Classes** gives every player a class choice that shapes abilities and passives. Juggernauts carry Momentum checkpoints and Burden's heavy chassis, loosen the swing with Offload and spend the bar on Onslaught; Legionaries rotate Gladius/Scutum stances, cash in Impetus/Testudo flip buffs, and swear the Aequilibrium oath; Gamblers bet on Crooked Dice with Streak loading the odds, spin the free Roulette wheel and go All In for heavier crits. First join opens the picker; the choice persists while stacks and cooldowns stay transient.

## Classes

- **Juggernaut** (Frontline bruiser) — Momentum checkpoints and Burden's heavy chassis, Offload to loosen the swing for a moment, Onslaught to spend the bar on damage.
- **Legionary** (Sword and shield) — Rotate stances, Impetus/Testudo flip buffs, Aequilibrium oath ultimate. Enters in Scutum.
- **Gambler** (Double or nothing) — Crooked Dice: 15% crit at ×1.5 with a fixed 5% betrayal at ×0.5; Streak adds +15% crit per non-crit full hit up to a 95/5 lock. Free Roulette wheel on R; All In grants 15s of ×1.75 crits on V.

Crits are mod-owned: vanilla jump crits are retired, and every full-charge landed hit rolls a flat 10% chance at ×1.25 for any player (classless included). A class's crit rule overrides it — the Gambler's Crooked Dice is the first.

## Requirements

- Minecraft **1.21.11** only, **Fabric Loader 0.19.5+**, **Fabric API**, **Java 21**
- Install the same jar on **both client and server** (server-required)
- No Forge/Quilt. No 1.20.x backports in 0.x.

## Installation

1. Install Fabric Loader for 1.21.11 + Fabric API.
2. Download the jar matching your game (`infusedclasses-<mod>+1.21.11.jar`).
3. Place it in `mods/` on client and server, then launch. The picker opens on first join.
4. Reset class data after updates if told to: `/class select <player>`.

**Manual installs only**, no auto-updater; Modrinth + CurseForge listings arrive as manual uploads on the Beta channel.

## Full documentation

📖 **[Infused Classes guide](https://github.com/Hunter1712/infusedclasses/tree/main/docs/guide)** — installation, classes, stances & passives, commands, configuration, compatibility, version policy, modpack notes and FAQ.

## Links

- **Guide / wiki:** https://github.com/Hunter1712/infusedclasses/tree/main/docs/guide
- **Source:** https://github.com/Hunter1712/infusedclasses
- **Issues:** https://github.com/Hunter1712/infusedclasses/issues
- **Changelog:** https://github.com/Hunter1712/infusedclasses/blob/main/CHANGELOG.md
- **Fabric API:** https://modrinth.com/mod/fabric-api

## Licence

**All Rights Reserved** — see the [LICENSE](https://github.com/Hunter1712/infusedclasses/blob/main/LICENSE). Modpack inclusion with credit is permitted; redistribution and derivatives require permission.
