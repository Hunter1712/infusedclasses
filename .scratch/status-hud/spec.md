# Spec: Status HUD (the slot strip)

Status: superseded by ADR-0019 — the whole HUD chain (draw layer, client config file, snapshot payload and sync) scrapped from the build pending a later implementation

## Problem Statement

Every bit of a class's live state lives in server-side transient runtime: cooldown lanes, ability windows (Rush stride, Rend empower, Impetus, Testudo, Aequilibrium, the Roulette outcome, All In), Momentum stacks with their grace, the Gambler's Streak, the Legionary's stance. The client learns none of it, so a player tracks all of it in their head — when a lane comes back, whether the next hit is still loaded, whether Testudo's catch is open, what the wheel spun. Chat speaks only at fire time, and in whole seconds. The one HUD the mod ever specced (the Bulwark effect-row overlay, `.scratch/passive-hud`) died with the effect-row approach; nothing on screen shows state today.

## Solution

- **A client status strip**: one vertical strip of vanilla-slot-style chips, anchored to a screen corner (bottom-right by default — chat owns the left), labels on the inner side mirrored per corner, no backdrop. Each chip is a badge box of uniform width — sized once for three-character badges, height 20 like a vanilla slot — plus its text label. Letters and numbers only; icons are a later pass.
- **The chip roster** (badges are slot and state names, never key bindings):
  - Every class: **SIG** signature lane (Rush 25s / Rotate 8s / Roulette 25s) and **ULT** ultimate lane (Rend 60s / the 50s oath lane / All In 75s), anchored always — dim while ready, lit while cooling, with a one-decimal countdown while cooling.
  - Windows ride beside their lane chip: Juggernaut's stride (5s, with `+` while the engage bonus is still unspent) and Rend's empower (6s while its next-hit bonus is unspent, countdown-only); Legionary's **IMP** (5s, pending-unspent only) and **TES** (1.5s) off the signature lane, **AEQ** (6s) off the ultimate lane; Gambler's outcome chip — a single-letter badge (`E`/`W`/`H`/`J`/`B`; never Zero) plus its 10s countdown — and **ALL** (15s) off the ultimate lane.
  - On-state chips: **MOM** Momentum (stack count plus grace countdown), **STR** Streak (count), and Legionary's anchored **GLA**/**SCU** stance. Juggernaut's Vigor never appears.
- **One snapshot, change points only**: a new S2C `PlayerStatusPayload` carries the strip's state at send time; the server emits it only when something a client cannot derive has changed (a fired ability, a melee settle that moved state, a decay tick that moved stacks, class switch, respawn, join). The client interpolates countdowns between snapshots.
- **Rendering is a decision**: `StatusHud.rows(status, elapsed)` is pure — given the last snapshot and the millis elapsed since it arrived, it returns the strip's rows; the HUD adapter draws them. No screen, player, or clock enters the decision.
- **Config is drawing only**: `hud.enabled` (default true) and `hud.corner` (default `bottom_right`) live in the client config file (owned by `.scratch/player-tools`); `enabled=false` stops drawing, nothing else. The strip hides on F1, while any screen is open, and for classless players.
- **Countdowns go precise**: one decimal everywhere, chat included — the shared `ChatLine.cooldown` line rounds `%.1fs` like the strip.

## User Stories

1. As any player in combat, I want my class's lane cooldowns on screen, so I know when my next ability is ready without counting.
2. As a Juggernaut, I want to see Momentum's stack count and when grace runs out, so I know what my next Rend is worth.
3. As a Juggernaut, I want the stride window and whether my engage bonus is still loaded, so I know if my next swing eats it.
4. As a Juggernaut, I want the empower window while Rend's next hit is still armed, so I know the bonus is coming.
5. As a Legionary, I want my current stance on the strip, so a flip is visible at a glance.
6. As a Legionary, I want the Impetus and Testudo windows with precise countdowns, so I can play the 5s and 1.5s catches on purpose.
7. As a Legionary, I want the Aequilibrium window on the ultimate row, so I know how long the oath lasts.
8. As a Gambler, I want my Streak count, so I can see the dice loading.
9. As a Gambler, I want the spin's outcome and its window on screen, so the verdict stays readable after the announcement fades.
10. As a Gambler, I want Zero to show no chip, so the dead band reads dead.
11. As a player with no class, I want no strip at all.
12. As a player, I want the strip in a corner out of the way (bottom-right by default), so chat and the hotbar stay untouched.
13. As a player, I want countdowns precise to a decimal — chat included — so the numbers actually tell me whether there is time.
14. As a modpack user, I want `hud.enabled=false` to stop the drawing without changing how the mod plays.
15. As a QA tester, I want the rows and the snapshot math asserted purely, so a regression shows up without a running client.

## Implementation Decisions

- New pure record `PlayerStatusPayload` (the snapshot and the wire payload in one, house `*Payload` naming): class id, per-lane remaining ms, per-window remaining ms plus their load flags, Momentum stacks plus grace remaining, stance, flip target and window, Streak, Roulette outcome. Static builder `of(classId, runtime, now)` reads only runtime verbs; new S2C type `infusedclasses:player_status` registered with its siblings in `InfusedClasses.registerPayloads`.
- The builder needs a few new mechanical reads on `PlayerRuntime`, one-liners in the ADR-0013 shape (never the private `Window`): remaining-millis verbs for the six ability windows (rush, rend, flip, roulette, aequilibrium, all-in; 0 when dead) and one grace-remaining read for a passive (0 when stacks are gone).
- `movedSince(prev)` on the snapshot: true when anything a client cannot derive changed — class id, each lane/window armed-or-not, load flags, stacks, stance, flip target, Streak, outcome — and false for pure time decay, so emission is "changed?", not "sent recently".
- New server-side `StatusSync` adapter: last-sent snapshot per player UUID (removed on disconnect, re-seeded on the shared-UUID respawn), rebuilds and sends only when `movedSince`. Call sites: both intent receivers (after their branch runs — a rejected press moved nothing and sends nothing), `settleAttackerHit` (after the settle lands), the server tick (after `PassiveApplier.apply`, which owns the decay move), `ClassSwitch.trySwitch` on an accepted switch, respawn, and join (join and respawn send unconditionally so a client always learns its class).
- New client store `client/StatusStore`: the last snapshot plus its receipt time; overwritten per payload, cleared on client disconnect. The element and the rows decision read only this.
- New pure `StatusHud.rows(status, elapsedMs)`: the whole strip as rows — roster per class id (empty for classless/unknown), row order (stance, signature row, ultimate row, state rows), values (countdowns `%.1fs` Locale.ROOT, stack counts; a ready lane renders dim with no number), riders, the `+` marker (only where the chip outlives the load: the stride rider; spent empower/IMP riders vanish), lit/dim state, and Zero drawn as no chip. Labels come from the class definition and ability constants where they exist (ability names, stance names, outcome labels), so copy cannot drift.
- Badges are fixed HUD vocabulary: `SIG`, `ULT`, `MOM`, `STR`, `GLA`, `SCU`, `IMP`, `TES`, `AEQ`, `ALL`; Roulette outcomes keep their single initials. They name slots and states — the strip never reads a KeyMapping, so remapped keys cannot make it lie. (`S`/`U` were an earlier pass; three-letter names superseded them.)
- `ChatLine.cooldown` switches to `%.1fs`; the strip and the info lines share the precision.
- Juggernaut Vigor (always-on, HP-derived) and Roulette Zero get no chip; the roster maps class → chips by the abilities the class carries, so a future ability adds a row rather than a branch.
- Client element `client/StatusHudElement`, registered via fabric-api's `HudElementRegistry` as its own element (`infusedclasses:status`): draws only when no screen is open, the HUD is not hidden (F1), a snapshot exists and its class id is non-empty; `hud.enabled=false` stops drawing only. The corner comes from `HudConfig`; the strip grows away from the anchored corner with labels on the inner side.

## Testing Decisions

Good tests assert the pure decisions with boundary values — no live clients, no game objects. Prior art: the ability tests, the runtime tests, the card tests.

- New `PlayerStatusTest`: the builder reads lanes/windows/flags/stacks at a `now` (armed vs ready, live payloads, unspent vs spent one-shots); `movedSince` false for pure time decay (rebuild one second later, nothing changed) and true for each discrete change (lane armed, window armed, stack bump, Streak move, stance flip, outcome set, class switch).
- New `StatusHudTest`: rows for each class at boundary snapshots — lane rows always present (dim ready / lit cooling), window riders exactly while their window is live, stride `+` dropping after the bonus is spent while the rider remains, empower/IMP riders vanishing on spend, Momentum row carrying stacks plus grace and vanishing at zero, Streak row only above zero, Zero rendering no outcome chip, classless rendering no rows, countdown formatting at 10_000/9_999/0 ms and clamping at zero, elapsed past a window dropping its row.
- Extend the protocol test for the new payload's codec round-trip (existing pattern).
- Extend the runtime tests for the new remaining/grace reads (0 when dead, exact values at the expiry boundary).
- Update any test pinning the old `%.0fs` cooldown line to the one-decimal text.
- Not unit-tested (house convention, manual dev-client pass via `gradle deployClient` and `/class select`): the element's drawing, corner mirroring, hide rules, and the emission wiring's live timing.

## Out of Scope

- Icons in chips (v1 is letters/numbers only), custom textures, animations, an in-game config screen or a toggle key.
- HUD rows for other passives (Vigor) or anything beyond the roster above; per-player layout beyond the two corners.
- Persisting HUD state across relog; the snapshot is session data.
- A Juggernaut ultimate: its `ULT` lane shows Rend until one exists; the roster is data-driven so that swap is a definition change.
- The server reading the client config (purely client-side, per the tools spec).

## Further Notes

- Slot names versus keys: the strip spells slot names (`SIG`/`ULT`). Copy (chat lines, picker descriptions) keeps physical-key notation; the guide tells players the strip names slots while Controls shows the real keys.
- The stride rider's `+` is the one "loaded hit" marker; Rend's empower and Impetus riders exist only while their one-shot is unspent, so they carry no marker of their own.
- Emission is guarded by `movedSince`: a rejected press, a spam swing, or a quiet tick sends nothing; join and respawn force one snapshot each.
- `.scratch/player-tools` issue 01 (the config file) should land before (or with) the client element issue here; defaults apply until then.
- ADR-0014 records the reversal (a HUD returns, as the slot strip) and `.scratch/passive-hud` points here as superseded.
