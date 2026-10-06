# 04: War Cry HUD row signal, store, and overlay

**What to build:** A Bulwark-style custom row titled `War Cry` (no `II` — Strength II is implied) showing seconds-left plus a drain bar. New S2C signal carrying War Cry expiry ticks, sent event-driven on fire and every proc (like the stacks signal, not tick-synced). Client store interpolates the countdown between messages; overlay row mirrors the `PassiveHud.Row` shape. The vanilla Strength icon stays alongside (accepted duplication — no per-effect hide available).

**Blocked by:** 03 (needs the fire and proc emission points)

**Status:** ready-for-human

- [x] New S2C payload (e.g. war-cry expiry ticks) registered in `InfusedClasses.registerPayloads`; emitted on War Cry fire and on every extension proc, plus cleared on class switch / respawn / effect loss so no stale row sticks around
- [x] Pure row decision mirroring `PassiveHud.rowFor` shape: title `War Cry`, seconds-left from expiry ticks, drain-bar fraction, hidden flag at zero/missing; covered in plain JUnit with no player or graphics object
- [x] Client store (sibling to `ClientPassiveStacks`) holds the last signal and ticks the displayed countdown down locally between messages; fake-clock assertions, no waiting
- [x] Overlay adapter draws verbatim whatever the decision returns: HUD strip slot plus inventory-box row in the existing `PassiveHudOverlay` look; draws nothing when hidden, when `hideGui` (F1) is set, or when the player has no class
- [x] Classless players emit nothing; beacon-granted Strength never feeds this row (only War Cry fire/proc events do)
- [ ] Overlay layout and pixels are manual-check only (see 05), not JUnit

## Comments

`WarCryExpiryPayload(expiryTicks)` + `ClientWarCry` (20 ticks/sec local countdown) +
`PassiveHud.warCryRow` reusing the `Row` shape (bar full at a fresh 8s window, capped when
extended). Review follow-up changed the effect-loss story: the server tick now zeroes the signal
edge-triggered when a signalled effect dies (flag + `pollWarCrySignalLost` on the runtime, pure
in booleans), so banked ticks cannot be resurrected by a later beacon; the overlay's effect gate
stays as a backstop. Switch/respawn zero the signal; respawn skips the packet for classless
players. `ProtocolTest` updated 5 → 7 channels. Overlay pixels are the 05 manual pass.
