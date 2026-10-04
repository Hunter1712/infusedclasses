# 04: Card lines carry style, card drawing moves behind one module

**What to build:** The picker screen currently re-derives meaning from rendered text — it compares a line against the literal "COMING SOON" to decide how to draw it — and it owns the frame, accent strip, locked veil, hover highlight and per-line colouring. Make a card line carry the colour and weight it needs, derived from its kind, so the screen never needs to know what a line says in order to draw it. Move the drawing rules into one module per card, leaving the screen owning only widget construction, hover position and layout.

From the player's side nothing changes: the picker looks and reads exactly as it does now.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] A card line carries the colour and weight the screen needs, derived from its line kind.
- [ ] The screen no longer compares any rendered text to decide how to draw it.
- [ ] The coming-soon notice is identified by its line kind, not by matching a string.
- [ ] Card frame, accent strip, locked veil, hover highlight and per-line styling live in one card module.
- [ ] The screen owns widget construction, hover tracking and layout, and nothing else about drawing.
- [ ] Locked classes stay unselectable and visually veiled.
- [ ] Line style is asserted across every line kind, including the notice, alongside the existing text and kind assertions.
- [ ] Existing presentation and tooltip assertions pass unchanged.
- [ ] No visible text, card layout, colour or sizing changes.