# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Class picks are gated by a server-issued, transient picker grant: a `select_class` intent that arrives without the server having opened the picker is ignored, so a modified client can no longer switch class mid-fight and reset cooldowns.
- Killing blows run the state-only melee plan: the hit that finishes a fight now grants Momentum, pays Onslaught's price, spends Impetus, moves Streak and plays the crit cue. The extra settle damage still drops on a kill.
- The settle bonus no longer scales with difficulty a second time (`scaling: never`), so a Gladius Legionary pays x1.2 of the landed base on every difficulty.
- Client HUD countdowns run on monotonic millis, so a system clock step cannot freeze or skip a countdown.
- `/class set` reports success or rejection to the command source instead of leaving the admin guessing; the picker footer names the `Select` button; and a press on a cooling Offload or Onslaught lane is silent even when the stack price is also short.
- Damage mults scale what a hit actually landed on the victim: armor, resistance and enchantments the base hit paid for are not paid again by the bonus, so a x1.25 crit on an armored target lands as x1.25 of the health the hit took, exactly as the balance model always priced it. The bonus hurt rides its own damage type (`infusedclasses:settle`, tagged to bypass armor, effects and shields, and to cost a player victim no second hunger exhaustion), and the `lastHurt` memory is restored by what the bonus actually took off, so a rejected or fully eaten bonus no longer inflates the window.
- Mod crits show their particle burst again: 1.21.11's own `Player.crit` is empty, so `CritCue` spawns `ParticleTypes.CRIT` at the victim beside the crit sound.
- Cooldowns, ability windows and passive grace run on a monotonic clock (`InfusedClasses.now()`, `System.nanoTime` millis) instead of wall time, so a clock step — a VM snapshot restore, an NTP correction — can no longer skip a cooldown or freeze one. The client HUD's countdowns stay client-local.

### Changed
- Picker cards take their class's colours (ADR-0032): every card tone — body tint, divider,
  tagline, class name, every item name (passive and active alike), checkpoint names, and the
  plain/hover border pair — derives from the class accent in one pure `CardPalette`. Hover
  brightens the border only: no wash over the body. Section headers stay white bold and
  descriptions of both sections share one subtle neutral (`#8C8C8C`, dim enough that accent
  names stay brightest); the backdrop deepens behind a subtle edge vignette. The card body
  left-aligns and a bar's checkpoint rows draw as indented `TIER` lines, so they hang off
  their rules line instead of reading as standalone passives.
  The actives section header reads `Actives`, parallel with `Passives`.
  The HUD, chat and wire keep their own colours.
- The cast fanfare becomes the evoker's own cast spell (`entity.evoker.cast_spell`) instead of
  the note-block pling: every fired ability still plays one shared sound, now one that reads as
  a cast, played at 1.6 volume so its low whoosh carries over combat; Rotate keeps announcing
  without a fanfare, and fizzles and rejects stay silent.
- Player copy pass: Momentum's card line splits into its rules line plus one row per checkpoint
  (`Steady at 15` …), each still generated from the bar's own numbers, so the bar scans row by row;
  Burden's card line drops its flavour sentence, and weight wording leaves the reader-facing prose
  (module javadocs, GLOSSARY, project description). Ids and `Tier.weight` are unchanged.
- Ability firing, window auras and the status snapshot get their decisions beside their
  constants (ADR-0029/0030/0031): the roster row carries its reused slot, its fanfare and its
  pure fire, so `BindMorph` is one slot walk and `SlotPresses` is one press; each window
  ability carries its own aura family and `ConditionalPassive` carries the rows its bar writes,
  so the applier is a loop; the snapshot composes the runtime's own `State` and `BarRead` reads
  and the tick's sequence is the named `TickPass`. No player-visible rule changes: the wire order,
  the HUD rows, the cooldown boundaries and the attribute reconcile are unchanged.
- `PROJECT_DESCRIPTION.md` catches up with Beta: current kits (Momentum checkpoints with Offload and Onslaught), Crooked Dice at 15% and All In at x1.75 on `V`, and no alpha contract.
- `ServerEvents` splits at its two bulky seams: `MeleeHook` owns the AFTER_DAMAGE body and `SlotPresses` the reused-bind presses (one press per slot, ADR-0029), leaving the file as lifecycle wiring and the one steady-state emission point, under the repo's ~150-line line. The settle's landed math is the new `HitMitigation`.

## [0.4.0] - 2026-10-19

### Added
- Status HUD returns as the left-centre text stack (ADR-0021): a vertical column of words on the left screen edge, vertically centred — each chip draws its ability, stance or state name with its countdown one space after it in whole seconds rounded up, and a ready lane dims and drops its number. Rows: the Legionary's stance, the signature lane, the ultimate lane, then the state rows (Momentum with its checkpoint word, Streak); window riders draw beside their lane's number as dotted counts (`Offload 10s · 5s`, the Roulette outcome keeping its band word: `· Jackpot 8s`). Driven by the `infusedclasses:player_status` snapshot sent only at change points, guarded by `movedSince`; text only — no icons, no textures, no column padding, no config file, no knobs. `docs/guide/hud.md` returns with it.

### Fixed
- The Momentum grace clock is visible while you keep hitting: a hit at the stack cap re-stamps the grace without moving the count, and the snapshot now carries the grace stamp so that re-stamp reaches the client (it used to run the displayed grace down to `0s` and leave it there mid-combat). The Momentum row also never prints `0s`: a stale snapshot draws bare stacks instead.
- A hit fully eaten by a raised shield earns nothing and spends nothing: the melee plan takes what the hit actually landed and short-circuits at zero, so blocked swings no longer grant Momentum stacks or Streak movement, and no longer consume Impetus or Onslaught's priced hit. A partially blocked hit still earns, and the stance and crit mults now scale what landed after shield and modifier reductions instead of the pre-shield base.
- Spear thrusts and mace smashes gate on their own swing: the swing charge gains a second capture at the head of the stab route, because both weapon families damage there and only the spear reaches `onAttack` (after the damage). Momentum, Onslaught pricing and the dice now read the weapon you are actually swinging, not the last ordinary swing — a held spear thrust at full charge earns like a sword, and poke spam earns nothing.
- `/class set <player> <class>` accepts the ids the suggestions offer: the argument is vanilla's identifier argument, so `infusedclasses:juggernaut` parses and sets the class, while an unregistered or locked id still answers `Unknown or locked class` and changes nothing.

### Changed
- Project status promoted from Alpha to Beta: the README and guide drop the alpha contract and its wipe/migration promises, the listing channel word becomes Beta, and the jar ships as 0.4.0.
- One unit per stat family (ADR-0028): damage power is now a post-mitigation multiplier composed on the landed hit — Burden, Gladius, Aequilibrium and Onslaught all convert their `+1` damage into `x1.15` at the iron-sword reference, folded into one combined mult with the stance, wheel, oath and dice rows (`MeleePlan.HitPlan.combined`); the always-on chassis row carries its melee mult beside the modifiers it writes (`ClassDefinition.AlwaysOn.dealMult`). Onslaught's mult pays on any landed hit while its window is live; the 2-stack price stays per full swing. Attribute stats stay flat in their own unit, chances stay percents, stacks stay integers, time stays seconds — one unit per family, never mixed again. Balance gains a target (±10% expected payout across classes at the bare and iron-armored references) and a first-cut sheet under `.scratch/balance-pass/`.
- Movement speed copy goes flat with its values (ADR-0028): Momentum reads `-0.005/-0.01/-0.015`, Scutum `-0.005` (was about -6%), Offload `+0.01` (was +10%), Haste `+0.02`; `ConditionalPassive.percent` is deleted, so no movement row claims a percentage again. The slows read heavier than the old percent intent; the tuning round left the grid as it reads.
- Balance tuning round on the sheet's candidate set (ADR-0028): Burden's swing penalty reads -0.4 (was -0.5), Gladius's glass tax x1.2 (was x1.1), Crooked Dice crits at 15% base (was 20%) with All In at x1.75 (was x2.0); Scutum's plate stays as it reads. Offense and defense are measured on one timeline (`.scratch/balance-pass/model.py`, armor read per hit, windows on their lanes) and the five rows land inside the ±10% band around the mean.
- One cast fanfare for every ability: the per-ability sounds (wind burst, ravager roar, beacon swell, level-up chime) retire for the one `AbilityApplier.CAST_SOUND`, the wheel's note-block pling — shortest and the highest-frequency press's own. Every fired ability resolves it at the shared play path; fizzles and rejects stay silent, and Rotate still announces its stance with no fanfare.
- Juggernaut reworked around Momentum as its one mechanic (ADR-0027): the bar runs 0-60, earns +1 on every full-swing hit dealt and +1 on every hit taken, holds for 5s quiet, then bleeds 1/s. Three checkpoints are read off the bar every tick — 15 Steady (+1 armor, -0.005 speed, +0.25 knockback resistance), 30 Braced (+3/-0.01/+0.5), 45 Rooted (+5/-0.015/+1.0); past 45 stays Rooted, and 45-60 is the actives' spend reserve. Burden is the always-on chassis (x1.15 damage, -0.4 attack speed). Offload (`R`) pays 5 stacks for 5s of +0.2 attack speed and +0.01 move speed on a 10s lane; Onslaught (`V`) needs 10 stacks, runs 15s or until the bar empties, deals x1.15 melee on every landed hit while each full swing spends 2 stacks, and stops the bar from feeding while it is live. Ability ids are `infusedclasses:offload`/`infusedclasses:onslaught`; the R/V keys and the `rush`/`rend` wire names stay.
- `/class set <player> <class>` takes a namespaced id in practice: like every vanilla command argument, a bare name resolves to the `minecraft` namespace, so type the full id (tab-complete writes it). The commands page says so; the install pages use `./gradlew` and the jar name a build actually produces, and the FAQ points at the chat line the rejected player sees instead of a log that never gets one.
- Vocabulary sweep (ADR-0021): `stride` and `empower` leave the code; comments, test names and the prototype speak the Rush window and the Rend window. Those nouns now retire with the abilities themselves (ADR-0027).

### Removed
- Rush and Rend retire with the Momentum rework (ADR-0027), along with their appliers and the `infusedclasses:rush_speed`/`infusedclasses:rend_*` modifier ids. Vigor is cut and no class registers an HP-scaled passive; the shared bucket machinery stays. Momentum's old all-at-once drop and `momentum_rooted` are gone: the tier is now read off the bar each tick and every tier carries knockback resistance.
- The charged bonus curve (`BonusCurve.bonusFor`) is deleted (ADR-0028): its only caller was double-gated on a full swing, so the curve had no reachable math. The full-swing gate survives as `FullSwing.isFull(charge)`, still shared by the stack feed, the crit roll and the Onslaught price. The `burden_damage`, `gladius_damage` and `aequilibrium_edge` modifier ids go with the damage flats they wrote, and `DamageSettle.dealBonus` goes with Onslaught's flat chip.
- Status HUD scrapped pending a later implementation (ADR-0019): the on-screen strip, its client config file (`config/infusedclasses.json`) and its `infusedclasses:player_status` snapshot signal all leave the build. Lane countdowns stay readable through `/class info`, and a press on cooldown still says nothing (ADR-0017's silence carried at ADR-0019). The ADR 0014/0017/0018 chain and the `prototype/strip-goes-bare` branch record what the next HUD builds on.
- The generated HUD icons (ADR-0021): the ten PNGs, `tools/generate-hud-icons.ps1` and `textures/gui/ability/` are deleted — the stack speaks words, so no GUI textures ship.

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
