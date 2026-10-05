# 04: Overlay adapter and server emit wiring

**What to build:** Thin client overlay adapter that draws verbatim whatever the 01 row decision returns, plus server emit wiring at the existing hit, tick-decay, and class-switch points.

**Blocked by:** 03.

**Status:** done

- [x] Overlay adopts the vanilla effect HUD: strip icon slots in the HUD, vanilla-metric boxes (`Name <ROMAN>`, `mm:ss`, no subline or bar, exactly like vanilla rows) appended in inventory; resistance sprite with text-only fallback if the lookup drifts on 1.21.11.
- [x] Adapter draws nothing when the decision says hidden, when `hideGui` (F1) is set, or when the player has no class; inventory stays visible with no extra double-draw handling.
- [x] Server emits the 02 signal only from the hit trigger path, the server-tick path when `tickDecayMoved` is non-empty, and the class-switch and respawn paths; classless players emit nothing.
- [x] In-game check via `gradle deployClient` plus `/class select <player>`: take hits to reach `Bulwark III`, watch the countdown drain, confirm the row hides at zero and never covers War Cry Strength.

## Answer

`client/PassiveHudOverlay` attached after `STATUS_EFFECTS`, offset by active-effect count; `gradle deployClient` shipped the jar. In-game visual check is yours: `/class select`, take hits to `Bulwark III`, confirm drain, hide at zero, no Strength overlap.
