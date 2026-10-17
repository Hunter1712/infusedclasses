# Spec: Ability HUD (the left-center text stack)

Status: implemented (ADR-0021, superseding ADR-0020 on the surface); in-game verification rides issue 05

## Problem Statement

Every bit of a class's live state lives in the server-side transient runtime: cooldown lanes, ability windows (the Rush window, the Rend window, Impetus, Testudo, Aequilibrium, the Roulette outcome, All In), Momentum stacks with their grace, the Gambler's Streak, the Legionary's stance. The client learns none of it, so a player tracks all of it in their head — when a lane comes back, whether the next hit is still loaded, whether Testudo's catch is open, what the wheel spun. Chat speaks only at fire time. ADR-0019 scrapped the previous HUD chain (the slot strip, then pills, then the bare zone stack) because every look kept failing the eye test; since then nothing on screen shows class state at all.

ADR-0020's icon stack failed that same test one look later: with the mod's own 16×16 art a glance could not tell Rush from Rend, and ten similar plates left the numbers doing all the work. A browser prototype (`build\hud-text-prototype.html`, six variants, pictured before code moved) showed what does read: the ability, stance and state names themselves.

## Solution

- **The HUD is a text stack on the left edge**, the block vertically centered on the screen: every chip draws its name — `Rush`, `Rend`, `Rotate`, `Aequilibrium`, `Roulette`, `All In`, `Gladius`, `Scutum`, `Momentum`, `Streak` — in Title Case with one space before its countdown, then riders after the countdown as `·`-separated dotted numbers. Text only: no icons, no textures, no fills, no frames. The left-centre anchor is fixed; there is no config file and no second corner.
- **The gap is a single space**, not a padded column: rows read as written (`Roulette 13s` over `All In 34s`), so numbers are ragged down the stack. Column alignment was tried first and read as tab stops.
- **Timers are whole seconds**: the remaining time rounds up (`70s`, `9s`, `1s`), so a live countdown never reads `0`. A ready lane draws its name dimmed and prints no number; a live chip draws bright and prints its number. Ready is read from the missing number, live from its presence.
- **The roster is the full one the scrapped spec settled** (carried over intact):
  - Every class: the **signature lane** (Rush 25s / Rotate 8s / Roulette 25s) and the **ultimate lane** (Rend 60s / the 50s oath lane / All In 75s), one word each.
  - Riders ride their lane's row as bare dotted counts: Juggernaut's Rush window beside the Rush number (spent or loaded — no marker) and the Rend window beside the Rend number while its next hit is unspent; Legionary's Rotate rider (Impetus pending only; Testudo while the scutum catch is open); the Gambler's outcome rider — the band's word (`Edge`/`Ward`/`Haste`/`Jackpot`/`Bust`; never Zero) — and the Aequilibrium / All In riders off the ultimate lane.
  - On-state rows: **Momentum** (stacks · grace) and **Streak** (count), shown only above zero; the Legionary's stance as its own top row (`Gladius` / `Scutum`, no number). Juggernaut Vigor never appears.
- **No GUI art ships**: the ten generated PNGs, `tools/generate-hud-icons.ps1` and `textures/gui/ability/` are deleted; retuning the stack is editing words and gaps.
- **One snapshot at change points**: the S2C `PlayerStatusPayload` carries the HUD's state at send time; the server emits it only when something a client cannot derive changed (a fired ability, a melee settle that moved state, a decay tick that moved stacks, a class switch, respawn, join). The client interpolates countdowns between snapshots from the receipt time. The Momentum **grace stamp** is one of those discrete events: a hit at the stack cap re-stamps the grace without moving the count, and the stamp moving is what makes that visible.
- **Rendering is a decision**: `StatusHud.rows(status, elapsed)` is pure and returns the stack's rows; the HUD element draws them. Hiding is the adapter's: no class, F1, any open screen, no snapshot.

## User Stories

1. As any player in combat, I want my class's lane cooldowns on screen, so I know when my next ability is ready without counting.
2. As a Juggernaut, I want to see Momentum's stack count and when grace runs out, so I know what my next Rend is worth.
3. As a Juggernaut, I want the Rush window's countdown beside the Rush lane, so I know how long the engage bonus stays cashed-able.
4. As a Juggernaut, I want the Rend window while Rend's next hit is still armed, so I know the bonus is coming.
5. As a Legionary, I want my current stance on the stack, so a flip is visible at a glance.
6. As a Legionary, I want the Impetus and Testudo riders with countdowns, so I can play the 5s and 1.5s catches on purpose.
7. As a Legionary, I want the Aequilibrium rider on the ultimate row, so I know how long the oath lasts.
8. As a Gambler, I want my Streak count, so I can see the dice loading.
9. As a Gambler, I want the spin's outcome word and its window on screen, so the verdict stays readable after the announcement fades.
10. As a Gambler, I want Zero to show no rider, so the dead band reads dead.
11. As a player with no class, I want no HUD at all.
12. As a player, I want the stack at the left edge's vertical centre, so chat and the hotbar stay untouched.
13. As a player, I want countdowns in whole seconds, so the numbers read at a glance mid-fight.
14. As a player, I want every name on the stack to be the same word the fire lines and `/class info` say, so the stack needs no legend.
15. As a player, I want the momentum grace to stay honest while I keep hitting a mob, so the number never lies about a clock that was just re-stamped.
16. As a QA tester, I want the rows and the snapshot math asserted purely, so a regression shows up without a running client.

## Implementation Decisions

- Pure record `PlayerStatusPayload` (snapshot and wire payload in one, house `*Payload` naming): class id, per-lane remaining ms, per-window remaining ms plus their load flags, Momentum stacks plus grace remaining plus the grace stamp, stance, flip target, Streak, Roulette outcome. Static builder `of(classId, runtime, now)` reads only runtime verbs; S2C type `infusedclasses:player_status` registered with its siblings in `InfusedClasses.registerPayloads`.
- The builder needs mechanical reads on `PlayerRuntime`, one-liners in the ADR-0013 shape: remaining-millis verbs for the six ability windows (rush, rend, flip, roulette, aequilibrium, all-in; 0 when dead), one grace-remaining read and one grace-stamp read for a passive (0 when stacks are gone; the stamp is the clock's last re-stamp, compared for movement, never read as time).
- `movedSince(prev)` on the snapshot: true when anything a client cannot derive changed — class id, each lane/window armed-or-not, load flags, stacks, the grace stamp, stance, flip target, Streak, outcome — and false for pure time decay, so emission is "changed?", not "sent recently".
- Server adapter `StatusSync`: last-sent snapshot per player UUID (removed on disconnect, re-seeded on the shared-UUID respawn), rebuilds and sends only when `movedSince`. Call sites: both intent receivers (after their branch runs — a rejected press moved nothing and sends nothing), `settleAttackerHit` (after the settle lands), the server tick (after `PassiveApplier.apply`, which owns the decay move), `ClassSwitch.trySwitch` on an accepted switch, respawn, and join (join and respawn send unconditionally so a client always learns its class).
- Client store `client/StatusStore`: the last snapshot plus its receipt time; overwritten per payload, cleared on client disconnect. The element and the rows decision read only this.
- Pure `StatusHud.rows(status, elapsedMs)`: the whole stack as rows — roster per class id (empty for classless/unknown), row order (stance, signature row, ultimate row, state rows), chips carrying a label, a value, a lit flag and a rider flag. Values are whole-second countdowns rounded up (`Locale.ROOT`, `%ds`); a ready lane renders dim with no number; the Momentum grace number prints only while live (a stale snapshot draws bare stacks, never `0s`). Names are the glossary's own nouns in Title Case, never keys, so a remapped key cannot make the stack lie.
- Client element `client/StatusHudElement`, registered via fabric-api's `HudElementRegistry` as its own element (`infusedclasses:status`): draws only when no screen is open, the HUD is not hidden (F1), a snapshot exists and its class id is non-empty. Left edge pad 4 gui, block vertically centred, rows 10 gui tall with a 2 gui gap (12 pitch); each chip draws as one plain string — its word, one space, its number, riders inline, each led by the dotted separator. Text is shadowed, lit white and dim grey; no blit, no fill, no texture.
- No config file: the build ships zero player-editable knobs; hiding rules and the anchor are constants in the element.
- The prototype for the look lives on the `prototype/hud-text-words` branch (six variants; its own copy of the deleted icons keeps the baseline variant rendering).

## Testing Decisions

Good tests assert the pure decisions with boundary values — no live clients, no game objects. Prior art: the ability tests, the runtime tests, the card tests.

- `PlayerStatusTest`: the builder reads lanes/windows/flags/stacks at a `now` (armed vs ready, live payloads, unspent vs spent one-shots) plus the grace stamp and its pinning across a quiet second; `movedSince` false for pure time decay (rebuild one second later, nothing changed) and true for each discrete change, including a **re-stamp at the stack cap** (stacks equal, stamp moved — the mid-combat bug this spec fixes) and each lane armed, window armed, stack bump, Streak move, stance flip, outcome set, class switch.
- `StatusHudTest`: rows for each class at boundary snapshots — lane rows always present (dim ready / lit cooling with their word), rider windows exactly while live as `·`-shaped bare chips, the Rush rider identical spent or loaded (no `+` survives), Rend/Impetus riders vanishing on spend, the outcome rider carrying each band word and Zero rendering none, Momentum carrying `stacks · grace` and vanishing at zero and never printing `0s`, Streak only above zero, classless/unknown rendering no rows, whole-second rounding at 10_000/9_999/1_000/999/1 ms, and elapsed past a window dropping its rider.
- Extend the protocol test for the payload's codec round-trip (including the grace stamp).
- Extend the runtime tests for the returned remaining/grace reads (0 when dead, exact values at the expiry boundary) and the stamp read (stamped value, 0 after clear, 0 when never triggered).
- Not unit-tested (house convention, manual dev-client pass via `gradle deployClient` and `/class select`): the element's drawing and spacing, anchor and hide rules, and the emission wiring's live timing.

## Out of Scope

- An in-game config screen or a toggle key; per-player layout beyond the fixed left anchor.
- HUD rows for other passives (Vigor) or anything beyond the roster above.
- Persisting HUD state across relog; the snapshot is session data.
- A Juggernaut ultimate: its ultimate lane shows Rend until one exists; the roster is data-driven so that swap is a definition change.
- Icons: a future icon pass is its own decision with its own prototype, not a residue of this one.

## Further Notes

- Words versus keys: the stack names abilities by their glossary nouns; copy (chat lines, picker descriptions) keeps physical-key notation, and the guide tells players the stack speaks names while Controls shows the real keys.
- No "loaded" marker survives: the Rush window's count reads the same spent or loaded; the Rend window's rider still vanishes on the spend (it exists only while unspent), and Impetus likewise. The band word is the only rider word.
- Emission is guarded by `movedSince`: a rejected press, a spam swing, or a quiet tick sends nothing; join and respawn force one snapshot each; a hit at the stack cap sends exactly one (the stamp).
- History: ADR-0020's icon look held for one review pass and is superseded by ADR-0021; the scrapped chain's archive lives at commit 4b5069e~1, the strip prototypes on `prototype/strip-goes-bare`, and the text-look prototype on `prototype/hud-text-words`.
