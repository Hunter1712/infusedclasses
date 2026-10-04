# 04: Card lines carry style, card drawing moves behind one module

**What to build:** The picker screen currently re-derives meaning from rendered text — it compares a line against the literal "COMING SOON" to decide how to draw it — and it owns the frame, accent strip, locked veil, hover highlight and per-line colouring. Make a card line carry the colour and weight it needs, derived from its kind, so the screen never needs to know what a line says in order to draw it. Move the drawing rules into one module per card, leaving the screen owning only widget construction, hover position and layout.

From the player's side nothing changes: the picker looks and reads exactly as it does now.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [x] A card line carries the colour and weight the screen needs, derived from its line kind.
- [x] The screen no longer compares any rendered text to decide how to draw it.
- [x] The coming-soon notice is identified by its line kind, not by matching a string.
- [x] Card frame, accent strip, locked veil, hover highlight and per-line styling live in one card module.
- [x] The screen owns widget construction, hover tracking and layout, and nothing else about drawing.
- [x] Locked classes stay unselectable and visually veiled.
- [x] Line style is asserted across every line kind, including the notice, alongside the existing text and kind assertions.
- [x] Existing presentation and tooltip assertions pass unchanged.
- [x] No visible text, card layout, colour or sizing changes.