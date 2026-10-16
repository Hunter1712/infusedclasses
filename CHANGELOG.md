# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Gambler: Crooked Dice (20% crit at x1.5, fixed 5% betrayal at x0.5) with Streak loading the crit band to a 95/5 lock, the free Roulette wheel (`R`, seven bands, 25s lane) and the All In ultimate (`G`, 15s of x2.0 crits, 75s lane).
- Mod-owned crits: vanilla jump crits are retired for every player; full-charge landed hits roll a universal 10% crit at x1.25 (classless included), with a per-class crit rule seam.
- Future Modrinth/CurseForge listing skeleton: single project page (Alpha channel, manual jars), version-suffixed jar name reserved for listing time (`infusedclasses-<mod>+1.21.11.jar`), in-repo player guide, listing body and changelog. No upload yet.

### Changed
- License replaced with All Rights Reserved (modpack inclusion with credit permitted; redistribution and derivatives require permission). See the player guide modpack notes.

## [0.1.0] - 2026-10-12

### Added
- Alpha: Juggernaut (Rush engage with cleanse, Momentum armor on landing hits, Rend execute, Vigor HP-scaled pair) plus Legionary (Rotate stances, Impetus/Testudo flip buffs, Aequilibrium oath).
- Focused picker: one large card per page with full kit printed on the card, server-validated picks, reopen on current page.
- Gamemaster-only `/class set` plus testing-only `/class select`.

### Notes
- Alpha contract: wipes and class resets at any time in 0.x, no data migration until beta. Manual installs only, 1.21.11 locked, server plus client install.
