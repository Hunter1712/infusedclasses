# ADR-0032: The card takes its class's colours

Status: accepted

## Context

The focused picker card was class-blind: every class drew the same blue ability rows, the same
white name and the same gray border, and only the 3px top strip carried the class accent. The
copy pass then split Momentum's bar into one row per checkpoint, and those rows shared the
passive style and the centered alignment — so they read as three standalone passives instead
of one bar's tiers. A grilling session (Oct 19 2026) settled the rework: the accent should seed
the card's colours, the base surface should be rethought, and the card should left-align with
checkpoints indented under their bar.

## Decision

1. **One palette module.** `CardPalette` derives every card tone from the class accent in one
   pure place: body = accent blended 6% into near-black; divider 45% over the dark base;
   tagline the warm mood tone nudged 12% toward the accent; class name and every item name
   (passive and active alike) = the
   accent lightened to a 0.55 luminance floor; checkpoint names = that legible accent dimmed;
   plain border 20% accent, hover border the legible accent with no body wash: hovering
   brightens only the edge. Section headers stay white bold and
   descriptions never take a line's colour: passive, checkpoint and active descriptions all
   draw one shared subtle neutral (`descText`, `#8C8C8C` — dim enough that every accent name
   stays brightest). The
   renderer keeps no colour constants of its own.
2. **A checkpoint is a line kind.** `LineKind.TIER` replaces the plain passive kind for
   checkpoint rows, and `CardLayout.rowIndent(kind)` decides the indent, baked into each
   `TextRow` — the renderer never re-derives it from the kind.
3. **The body left-aligns.** Every card line starts at `CardLayout.bodyLeft` (the card's
   padding), so indented tiers visibly hang under their bar's rules line; the name and tagline
   join the same column, and the divider already spanned the inner width.
4. **The backdrop deepens.** A darker gradient plus a stepped edge vignette sits behind the
   cards; it is accent-independent and belongs to the screen, not the palette.

## Consequences

- A new class gets its whole look from `accentColor()` alone; there is nothing else to fill in.
- Tuning a tone is one constant in `CardPalette`; every card moves together.
- `CardPaletteTest` pins derivation rules (the legibility floor, the tint direction, the dim
  order, the shared description tone), not hexes; `ClassPresentationTest` pins kind + accent
  → style; `CardLayoutTest` pins the checkpoint indent. `ClassCardRenderer` loses
  `BODY_COLOR`, `DIVIDER_COLOR`, `TAGLINE_COLOR` and the `dimmed` helper: a description tone
  is the palette's decision, not a per-row dim.
- The HUD, the chat lines and the wire keep their own colours: this decision is the picker
  card only.
