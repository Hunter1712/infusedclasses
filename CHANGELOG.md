# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-10-17

### Added
- Status HUD: a client slot strip of vanilla-slot-style chips anchored to a screen corner (bottom right by default) — lane countdowns (`SIG`/`ULT`), rider windows with their load markers, `MOM`/`STR`/`GLA`/`SCU` state chips and the wheel's outcome initial, all `%.1fs` precise and driven by server snapshots sent only at state change points (`infusedclasses:player_status`), so a quiet tick costs no packets. The strip hides on F1, while a screen is open, and for classless players; `hud.enabled=false` stops it, `hud.corner=bottom_left` moves it. See the guide's HUD page.
- Player commands: bare `/class` and `/class info` are open to everyone and read for the invoking player (class line, per-lane `%.1fs` countdowns, the Legionary's stance, `No class chosen` for classless); `/class pick` reopens your own picker when you have cheats. `set` and `select` keep their gamemaster gates.
- Client config file: `config/infusedclasses.json` (written with defaults on first run) with exactly `hud.enabled` (default true) and `hud.corner` (default `bottom_right`, or `bottom_left`); purely client-side, drawing only, never a crash on bad input. Documented in the guide.

### Changed
- The ultimate gets its own bind: `key.infusedclasses.signature` (default `R`) and `key.infusedclasses.ultimate` (default `V`) replace the old `rush`/`rend` key ids and their ability-named labels — anyone who had rebound the old keys starts from the new defaults once (accepted one-time reset in 0.x). Controls labels read "Signature" and "Ultimate"; copy names default keys (`(R, …)`/`(V, …)`); the HUD badges are slot names only.
- Roulette re-cut to six stat bands: Edge 25% (x1.25), Ward 25% (+2 armor), Haste 25% (+0.02 speed), Jackpot 10% (all three), Bust 10% (x0.75), Zero 5% (dead). The three 20% stat bands became 25% and the 8%/4% bands became 10%/5% once the Vigor (heal 3) and Jackpot-heal (heal 4) bands left the wheel; the same 10s window, 25s lane, free spin and deal mults stay. No healing exists anywhere in the mod after this.
- Chat cooldown lines moved from whole-second rounding to one decimal, matching the strip.

## [0.2.0] - 2026-10-16

### Added
- Mod icon (`assets/infusedclasses/icon.png`, 128x128) wired in `fabric.mod.json`; shows in Mod Menu.
- Gambler: Crooked Dice (20% crit at x1.5, fixed 5% betrayal at x0.5) with Streak loading the crit band to a 95/5 lock, the free Roulette wheel (`R`, seven bands, 25s lane) and the All In ultimate (`G`, 15s of x2.0 crits, 75s lane).
- Mod-owned crits: vanilla jump crits are retired for every player; full-charge landed hits roll a universal 10% crit at x1.25 (classless included), with a per-class crit rule seam.
- Future Modrinth/CurseForge listing skeleton: single project page (Alpha channel, manual jars), version-suffixed jar name reserved for listing time (`infusedclasses-<mod>+1.21.11.jar`), in-repo player guide, listing body and changelog. No upload yet.

### Changed
- License replaced with All Rights Reserved (modpack inclusion with credit permitted; redistribution and derivatives require permission). See the player guide modpack notes.

### Fixed
- Full swings are recognised again: vanilla zeroes the attack-strength ticker before a hit lands, so the hit hook always read a charge of 0 — no crit was ever rolled and Momentum, Vigor and Rend stacks plus the Rush engage bonus never grew. The swing's charge is now captured before the reset.
- Bonus melee steps land: vanilla drops any same-tick follow-up hurt that does not beat the base hit, which silently ate a crit's extra (x1.25 on a wood sword added nothing) and would have eaten Rend's bonus the same way. Extra steps now apply through vanilla's silent re-hit branch, so armor, death protection and kill credit stay intact and the numbers read as designed.

## [0.1.0] - 2026-10-12

### Added
- Alpha: Juggernaut (Rush engage with cleanse, Momentum armor on landing hits, Rend execute, Vigor HP-scaled pair) plus Legionary (Rotate stances, Impetus/Testudo flip buffs, Aequilibrium oath).
- Focused picker: one large card per page with full kit printed on the card, server-validated picks, reopen on current page.
- Gamemaster-only `/class set` plus testing-only `/class select`.

### Notes
- Alpha contract: wipes and class resets at any time in 0.x, no data migration until beta. Manual installs only, 1.21.11 locked, server plus client install.
