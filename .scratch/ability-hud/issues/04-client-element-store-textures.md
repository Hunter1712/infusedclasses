# 04: Client element, store and textures

Status: done

## Context

The adapter half: a client store for the snapshot, a fabric HUD element that draws the rows at the left edge's vertical centre, and the ten generated 16×16 icons.

## Goal

- `client/StatusStore`: last snapshot plus receipt time; `accept` overwrites both; `elapsed(now)` feeds the rows decision; `clear` on client disconnect.
- `client/StatusHudElement` implementing fabric's `HudElement` (note: this fabric-api build's signature is `render(GuiGraphics, DeltaTracker)`): hide when a screen is open, `hideGui` is set, no snapshot exists, or the class id is empty; otherwise draw the rows — pad 4 gui from the left edge, block vertically centred, rows 16 gui tall with a 2 gui gap, icon then text with a 4 gui gap.
- Icon drawing: `graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, 16, 16, 16, 16, color)`; lit chips full white, dim chips a 50% grey tint. Text is shadowed, lit white and dim grey; the row's text vertically centres on the icon.
- Register the element (`infusedclasses:status`, add last) and the payload receiver in `InfusedClassesClient`; clear the store on disconnect.
- `tools/generate-hud-icons.ps1` draws the ten icons from pixel-data strings via System.Drawing into `src/main/resources/assets/infusedclasses/textures/gui/ability/`: `rush`, `rend`, `rotate`, `aequilibrium`, `roulette`, `all_in`, `gladius`, `scutum`, `momentum`, `streak`.

## Done when

Icons exist and read correctly as PNGs, `gradle build` is green, and `gradle deployClient` puts the jar in the live instance for a dev-client pass.

## Comments

Implemented: `StatusStore`, `StatusHudElement` (left edge pad 4, block vertically centred, 16 gui rows, dim `0xFF808080` tint on ready icons), receiver and element registration, and `tools/generate-hud-icons.ps1` producing the ten PNGs. Icons eyeballed from a preview sheet during generation; `gradle deployClient` copied `infusedclasses-0.3.0.jar` into the live instance.

Superseded in look by 06 (ADR-0021): the element now draws text columns at pitch 12 with no blit, and the icons, their script and `textures/gui/ability/` are deleted. The store, the hide rules and the receipt-time interpolation carried over unchanged.
