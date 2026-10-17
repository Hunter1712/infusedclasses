# ADR-0018: The strip goes bare — a stack in a screen zone

Status: superseded by ADR-0019 (the whole chain leaves the build; the HUD is scrapped pending a later implementation)

## Context

Three pill builds failed the eye test: side-labeled boxes read as debug output
(ADR-0017's verdict), the pill remake still drew boxes that sank into night
biomes, and a live session showed giant white frames whose corners landed
exactly on pill corners — our own math cannot draw one, and boxes were the one
part of the design that kept losing. With the constraints locked, a throwaway
browser prototype priced five bare-text shapes side by side — token column,
countdown ladder, single sweep line, glyph meters, naked clocks — and the
single sweep won (`build\hud-fit-prototype.html`, branch
`prototype/strip-goes-bare`). In-game, that sweep's margin then failed twice:
the dot sat glued to the badge after it, and at gui scale 3 the fighting-state
roster (~161 gui) outran the margin past the hotbar's border (~131 gui) and
clipped its tail every held-stack fight. A second prototype round priced five
fit strategies, and the stripping-back came from outside instead: a Hud Texts
screenshot showed bare text anchored into flush screen zones, line per row,
right-aligned at the pad — the same vocabulary we had built, anchored where a
width ceiling cannot exist. The zone stack won.

## Decision

1. **The strip is bare text in a screen zone**: no fill, no outline, no
   sprite — not one rectangle exists between the rows decision and the screen.
   One line per row: a ready lane draws its bare badge dim, a cooling or live
   chip its bright badge and number, riders ride their lane's line. The dot
   and the sweep retire with the hotbar anchor — a stack does not hand off.
2. **The stack lives in a flush screen zone**: flush 2 gui from the screen's
   chosen edge, the block vertically centered on the screen, one line per row
   top-down with the signature row leading. `hud.corner` names zones now:
   `left` (default, left-aligned on a shared edge) or `right` (mirrored,
   right-aligned). The old hotbar spellings — `bottom_left`, `bottom_right`,
   `center_left`, `center_right` — still parse and carry their side, so a
   pre-zone file upgrades without a fallback log.
3. **Config owns size**: `hud.scale` (clamped 0.5–6.0, default 1.0) joins
   `hud.enabled` and `hud.corner`; a knob the file never mentions is not a
   fallback, so old files stay quiet on startup. Everything is measured in
   scaled gui units beside vanilla geometry, so every gui scale and every
   `hud.scale` lands the same frame.
4. **The producer stands**: `StatusHud.rows` and its JUnit pins are untouched
   — across four builds the one half that never failed.
5. ADR-0017's silence-on-cooldown and compressed numbers carry over: a press
   on cooldown says nothing; whole seconds at ten and above, tenths below.

## Consequences

- The strip leaves the hotbar it has hugged since ADR-0014; nothing from that
  chain's geometry (HOTBAR_WIDTH, xp-bar clearance) applies any more, and the
  anchor vocabulary moves from "hotbar's side" to "screen zone".
- A zone line grows into the open screen, so the margin ceiling that killed
  the sweep is structurally gone: no roster can clip, and no fit strategy is
  needed at any gui scale or `hud.scale`.
- The Hud Texts mod itself is Forge 1.21.1 and can never run here; it is a
  design reference, not a dependency. Its zone discipline is what the strip's
  `hud.corner` now names.
- The fit-strategy prototypes (edge pin, economy, wrap) live on
  `prototype/strip-goes-bare` unchosen; a future revisiting starts from the
  zone stack, not from them.
- If any frame artifact remains on screen over a box-less strip, its
  ownership is proven external (combat-dummy, Jade, sodium, or fabric-api) —
  the strip still draws nothing a frame could belong to.
- The prototype's losing variants live on `prototype/strip-goes-bare`, not in
  main; main keeps only the stack.
