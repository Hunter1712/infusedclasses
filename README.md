# Infused Classes

Character classes for Minecraft (Fabric). **Alpha — manual builds only.**

- Juggernaut: Rush engage with cleanse, Momentum armor on landing hits, Rend execute to spend it.
- Legionary: rotate stances, Impetus/Testudo flip buffs, Aequilibrium oath.
- Gambler: Crooked Dice crits with Streak loading the odds, free Roulette wheel, All In ultimate.

Crits are a mod rule: vanilla jump crits are retired, and every full-charge landed hit rolls a flat 10% chance at x1.25 (classless players included), with a per-class crit rule on top — the Gambler's Crooked Dice is the first.

## Requirements (0.x locked)

- Minecraft **1.21.11** only (`~1.21.11` in `src/main/resources/fabric.mod.json:23`)
- Fabric Loader `>=0.19.5`, Fabric API, Java 21
- No Forge/Quilt support. No 1.20.x backports in 0.x. A 26.x port comes later as a separate branch.

## Install (manual, forever)

1. Install Fabric Loader for 1.21.11 + Fabric API.
2. Build or fetch the jar: `gradle build` produces `build/libs/infusedclasses-0.2.0.jar` (`gradle.properties:8`, gitignored via `.gitignore:2`). At listing time jars carry the game version (`infusedclasses-<mod>+1.21.11.jar`) so a future 26.x jar slots onto the same page.
3. Drop the same jar in `mods/` on **both client and server** (server-required; the supported setup installs both sides). For the live dev instance: `gradle deployClient` (`build.gradle:50-65`).
4. Reset class data after updates if told to: `/class select <player>`.

No auto-updater. Modrinth + CurseForge listings (Alpha channel) are future manual uploads — this repo stays the source of truth.

## Alpha contract

- **Wipes/resets at any time in 0.x.** Worlds may break between alpha jars. No data migration until beta.
- Balance/content is out of scope for alpha success; crash-hunt + core loop feel is the goal.
- Support cap is best-effort. Off-topic reports (wrong loader/MC version, no log) close as wontfix.

## Feedback

Single funnel: [GitHub Issues](https://github.com/Hunter1712/infusedclasses/issues). Include:

- `infusedclasses` version (jar name), MC / loader / Fabric API versions
- `logs/latest.log` (crash section, not screenshots)
- Repro steps (class, stance/passive, server or client)

Homepage/sources: https://github.com/Hunter1712/infusedclasses

## Draft listing blurb (Modrinth/CurseForge, when listed)

> Infused Classes — ALPHA. Fabric 1.21.11 + Java 21 + Fabric API required, server + client install. Manual installs only. Expect wipes and class resets between 0.x updates with no migration. Report crashes with latest.log to GitHub Issues.

Icon: `src/main/resources/assets/infusedclasses/icon.png` (128x128, wired in `fabric.mod.json`). Listing stays unlisted until beta — prepped docs below, no upload yet.

## Player guide (prepped for listing, source of truth)

- [Installation](docs/guide/installation.md) · [Classes](docs/guide/classes.md) · [Stances & passives](docs/guide/stances-passives.md) · [Status HUD](docs/guide/hud.md) · [Commands](docs/guide/commands.md)
- [Configuration](docs/guide/configuration.md) · [Compatibility](docs/guide/compatibility.md) · [Version policy](docs/guide/version-policy.md) · [Modpack authors](docs/guide/modpack-authors.md) · [FAQ](docs/guide/faq.md)

## License

All Rights Reserved — see `LICENSE`. Modpack inclusion with credit is permitted; redistribution and derivatives require permission.
