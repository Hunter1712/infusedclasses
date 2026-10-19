# Spec: Card pass — class colours, left-aligned body, hanging checkpoints

Status: done

## Goal

A grilling session (Oct 19 2026) settled the picker-card rework the copy check surfaced:
the Momentum checkpoints read as standalone passives, and the card was class-blind besides
its top strip. The card now takes its class's colours, every line left-aligns and each
checkpoint row hangs indented under its bar.

## Decisions

- **One palette.** `CardPalette` derives every card tone from the class accent: body = accent
  blended 6% into near-black; divider 45% over the dark base; tagline a warm tone nudged 12%
  toward the accent; class name and every item name — passive and active alike — the accent
  lightened to a 0.55 luminance floor; checkpoint names that legible accent dimmed; plain
  border 20% accent, hover border the legible accent with no body wash: hovering brightens
  only the edge (the 10% wash read too bright at the card check).
  Section headers stay white bold; every description — passive, checkpoint or active — draws
  the palette's one shared subtle neutral (`descText`, #8C8C8C, dim enough that every accent
  name stays brightest). `ClassPresentation.styleFor(kind, accent)` and
  `cardStyleFor(accent, hovered)` are the only decisions; the renderer keeps no colours. The
  per-row `dimmed` helper retires.
- **A checkpoint is a kind.** `LineKind.TIER`; `CardLayout.rowIndent(kind)` decides the
  indent (one padding step, no bullet) and `wrapRows` bakes it into each `TextRow`, so the
  renderer never re-derives it.
- **The body left-aligns.** Every card line starts at `CardLayout.bodyLeft` (the card's
  padding) — name, tagline, section headers, rows — so indented tiers visibly stick out.
- **The backdrop deepens.** Screen-level darker gradient plus a stepped edge vignette
  (1.21.11's `fillGradient` is vertical-only, so the side edges step), accent-independent.
- **Scope.** Picker card and its backdrop only; HUD, chat and wire keep their colours.
  ADR-0032 records the decision.
- **Header copy.** The actives section header reads `Actives`, parallel with `Passives`.

## Testing

- `CardPaletteTest` pins the derivation rules (legibility floor, tint direction, dim order,
  the shared description tone), not hexes; `ClassPresentationTest` pins kind + accent → style,
  the `TIER` rows and the hover pair; `CardLayoutTest` pins the body-left anchor and indent;
  `ClassCardRendererTest` pins `TextRow.indent()` for `TIER` and non-`TIER` rows.

## Card check seen

Oct 19 2026: the three-card check passed — accent names, dimmer checkpoint names, shared
`#8C8C8C` descriptions, `Actives` header — and the hover re-check passed after dropping the
wash: hover brightens only the border. Instance log clean.
