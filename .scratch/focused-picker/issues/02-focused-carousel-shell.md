# 02: Focused carousel shell with navigation

**What to build:** Replace the fixed multi-card row with one centered scaled card per page that never overflows. Side arrows, clickable dots, and keyboard paging move between classes with wraparound; each page keeps a pinned Select control and the picker title, subtitle, footer hint, and dimmed backdrop.

**Blocked by:** 01: Delete unimplemented classes.

**Status:** done

- [x] One card visible per page, centered and fully on screen across narrow and wide window sizes
- [x] Side previous/next controls, clickable dots showing position and count, and keyboard paging all move pages
- [x] Paging wraps at both ends and reopening the picker lands on the player's current class page
- [x] Every page carries a pinned active Select control; picking still closes only on server confirm and a rejected pick keeps the picker open
- [x] Title, subtitle, footer hint, and dimmed backdrop retained

## Comments

Landed: `focusedCard`/`selectRect`/arrow/dot rects plus wrap math as pure renderer
decisions; screen is the thin adapter. Reopen page via `OpenClassScreenPayload`
current-class id (empty on first join). Keyboard uses the 1.21 `KeyEvent` record
(`keyPressed(KeyEvent)`), not the old int triple.
