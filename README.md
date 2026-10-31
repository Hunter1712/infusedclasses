# Infused Classes

Character classes for Minecraft (Fabric).

- Juggernaut: Momentum checkpoints and Burden's heavy chassis, Offload to ease the weight for a moment, Onslaught to spend the bar on damage.
- Legionary: rotate stances, Impetus/Testudo flip buffs, Aequilibrium oath.
- Gambler: Crooked Dice crits with Streak loading the odds, free Roulette wheel, All In ultimate.

Crits are a mod rule: vanilla jump crits are retired, and every full-charge landed hit rolls a flat 10% chance at x1.25 (classless players included), with a per-class crit rule on top — the Gambler's Crooked Dice is the first.

## Requirements 

- Minecraft **1.20.1**, **1.21.1** or **26.3** — one jar per version, each declaring `~<mc>` in its module's `src/main/resources/fabric.mod.json`
- Fabric Loader `>=0.19.5`, Fabric API, and the Java runtime your version needs (17 / 21 / 25)
- No Forge/Quilt support. **1.21.11 is no longer supported** — see [docs/guide/version-policy.md](docs/guide/version-policy.md).

## Install

1. Install Fabric Loader + Fabric API for your Minecraft version.
2. Build or fetch the jar for it: `./gradlew build` produces one jar per supported version under `versions/<mc>/build/libs/infusedclasses-<mod>+<mc>.jar` (gitignored via `.gitignore:2`). Jars carry the game version (`infusedclasses-<mod>+<mc>.jar`) so each slots onto the same page.
3. Drop the same jar in `mods/` on **both client and server** (server-required; the supported setup installs both sides). For the live dev instance: `./gradlew deployClient` (`gradle/version-module.gradle`).
4. Reset class data after updates if told to: `/class select <player>`.

## Feedback

Single funnel: [GitHub Issues](https://github.com/Hunter1712/infusedclasses/issues). Include:

- `infusedclasses` version (jar name), MC / loader / Fabric API versions
- `logs/latest.log` (crash section, not screenshots)
- Repro steps (class, stance/passive, server or client)

Homepage/sources: https://github.com/Hunter1712/infusedclasses

## Player guide (prepped for listing, source of truth)

- [Installation](docs/guide/installation.md) · [Classes](docs/guide/classes.md) · [Stances & passives](docs/guide/stances-passives.md) · [Status HUD](docs/guide/hud.md) · [Commands](docs/guide/commands.md)
- [Compatibility](docs/guide/compatibility.md) · [Version policy](docs/guide/version-policy.md) · [Modpack authors](docs/guide/modpack-authors.md) · [FAQ](docs/guide/faq.md)

## License

All Rights Reserved — see `LICENSE`. Modpack inclusion with credit is permitted; redistribution and derivatives require permission.
