# Spec: Focused Picker Carousel

Status: done

## Problem Statement

The picker screen shows all classes side by side in one fixed-size row. On narrow or scaled windows the row overflows, so the first card bleeds off the left edge and the last card bleeds off the right edge, truncating names, taglines, and buttons. Full passive and active descriptions live only in hover tooltips, so the card itself never explains what a class does, and unimplemented locked classes occupy picker space with placeholder cards.

## Solution

A focused picker: one large fullscreen class card per page with full descriptions printed on the card and no tooltips anywhere. Side arrows, dots, and keyboard move between pages with wraparound. Unimplemented classes are deleted from the registry rather than shown as locked placeholders, so the picker only ever shows playable classes and a new class automatically gains its own page.

## User Stories

1. As a player opening the picker, I want one large readable card instead of a clipped row, so that nothing is cut off on my window size.
2. As a player, I want the focused card centered at most of the screen with the title, subtitle, and footer still visible, so that the picker still reads as class choice.
3. As a player, I want side previous/next controls, so that I can move between classes with the mouse.
4. As a player, I want keyboard page controls, so that I can flip classes without reaching for the mouse.
5. As a player, I want page dots showing position, so that I know how many classes exist and which one I am viewing.
6. As a player on the last page pressing next, I want to wrap to the first page (and vice versa), so that navigation never dead-ends.
7. As a Juggernaut viewer, I want Momentum plus its full picker copy on the card, so that I learn the trigger, bonus, cap, and drop without hovering.
8. As a Juggernaut viewer, I want the Vigor family plus its joined bonus descriptions on the card, so that one HP-scaled passive never looks like two entries.
9. As a Juggernaut viewer, I want Rush and Rend each with name plus full description on the card, so that cooldowns, windows, and cast words are visible up front.
10. As a Legionary viewer, I want the no-passives placeholder plus Rotate, Impetus, Testudo, and Aequilibrium each with full descriptions on the card, so that the stance tradeoff, switch buffs, and shared oath are visible up front.
11. As a player, I want no hover tooltips anywhere on the picker, so that descriptions are always visible rather than hidden behind hover.
12. As a player, I want a large Select control pinned to the card bottom, so that choosing is obvious on every page.
13. As a player, I want picking to still validate server-side and close only on confirm, so that a rejected pick keeps the picker open for retry.
14. As a player, I want no locked placeholder pages, so that I only flip through classes I can actually choose.
15. As a returning player with a saved choice, I want the picker to open on my current class page when reopened, so that I am oriented.
16. As a future-class designer, I want adding one class definition to add one picker page automatically, so that no layout code changes per class.
17. As a server admin, I want the select command suggestion list to contain only playable classes, so that locked ids are never offered.

## Implementation Decisions

- Seams under test (highest pure seams, existing preferred):
  - Seam A — card presentation decision: resolves the whole card (header text, detail lines with name plus description, accent, button data) from the class definition in one pure decision. Tooltip output and locked branches are removed here, not in callers.
  - Seam B — card renderer layout decision: resolves the focused card rectangle, inner text layout with word-wrap, and the select button rectangle from screen size in pure integer math. Replaces the fixed-size row math.
  - The picker screen remains the thin adapter: positions widgets from the renderer math, resolves one card model per frame, wires arrows, dots, keys, and the select intent. It reads what the decisions return and never branches on line text.
- Card detail lines carry name plus full description per passive and per active, reusing the existing picker copy generation so numbers cannot drift from behaviour. HP-scaled passives in one family continue to read as one entry with joined descriptions.
- Line kind continues to decide line style; description continuation lines draw with the style of their parent item kind.
- Focused layout is one card per page, centered, sized as a proportion of screen size with minimum padding, so it cannot overflow like the old fixed row. Title, subtitle, dimmed backdrop, and footer hint are kept.
- Navigation is side previous/next controls plus page dots plus keyboard paging, with wraparound at both ends. Dots are controls, not decoration.
- The select control label and active state come from the existing picker button decision; with locked classes gone every visible page resolves to the playable form.
- Unimplemented classes are deleted from the class registry (definitions plus their locked-only validation branch and locked-only tests). Playable validation (a playable class must carry actives) stays.
- Glossary follow-ups (not this spec's behaviour, but terminology drifts without them): locked class, notice, card lines name-only wording, card tooltip, and picker screen row wording need revision once this lands.

## Testing Decisions

- Good tests assert external behaviour through the pure decisions, not rendering internals: given a class definition, the resolved card carries the expected detail lines and button data; given a screen size and page, the resolved rectangles stay on screen and inside each other. No game bootstrap, no graphics objects.
- Tested: card presentation detail lines (names plus descriptions, grouping, headers, empty-passives placeholder), button data always in playable form, layout rectangles centred and contained across representative screen sizes, page index wraparound.
- Prior art: pure decision tests in the class presentation suite, pure integer layout tests in the card layout suite, and the single card-model agreement test. Locked-branch tests are deleted with the definitions, not migrated.
- Not tested: actual font rendering, pixel-perfect wrap breaks, key event plumbing, packet round-trips — manual verification in the dev client.

## Out of Scope

- Custom picker art, animations, sounds, or per-class splash images.
- Multi-card grids, scrolling lists, search, or filtering.
- Balance changes to passives, actives, cooldowns, or descriptions beyond moving existing copy onto the card.
- New playable classes beyond the current two.
- Config files or datapack-driven classes.
- Touch or gamepad-specific navigation.

## Further Notes

- The screenshot that motivated this showed the outer two cards of the four-card row clipped with truncated taglines and buttons; the focused layout removes the overflow class entirely rather than tuning fixed widths.
- Deleting rather than hiding unimplemented classes is deliberate per grilling: no veil, no coming-soon notice, no locked button anywhere.
