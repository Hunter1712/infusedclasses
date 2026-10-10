# 03: On-card descriptions, zero tooltips

**What to build:** Print the full class kit on the focused card so nothing hides behind hover. Every passive shows name plus full picker copy and every active shows name plus full description with wrapping; all hover tooltips are deleted from the picker.

**Blocked by:** 02: Focused carousel shell with navigation.

**Status:** done

- [x] Juggernaut card shows Momentum name plus full picker copy and the grouped Vigor entry with joined bonus descriptions
- [x] Each active shows name plus full description with honest numbers (cooldowns, windows, cast words) and Legionary shows its no-passives placeholder plus all four actives
- [x] Long descriptions wrap inside the focused card without overflow or overlap with the pinned Select control
- [x] Description continuation lines draw with the style of their parent item kind
- [x] Zero hover tooltips on cards, select, arrows, or dots

## Comments

Landed: detail lines (`name — desc`) from the presentation decision reusing existing
copy generation; renderer wraps via `font.split` and clips before the pinned select.
Note: ticket text said "all three actives" but Legionary carries four after the
Aequilibrium change; the card renders whatever the registry carries, so no layout
change was needed.
