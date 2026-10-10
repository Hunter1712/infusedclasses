# 01: Delete unimplemented classes

**What to build:** Remove the unimplemented locked classes end to end so the picker only ever offers playable classes. After this ticket the picker flips through two pages, command suggestions offer only playable ids, and no veil, notice, or locked button remains anywhere.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Class registry holds only the two playable classes; no locked definitions remain
- [x] Picker shows only playable pages with matching dots; no locked page reachable by any navigation
- [x] No veil, coming-soon notice, locked button label, or locked tooltip reachable in the picker
- [x] Select command suggestions offer only playable ids; unknown ids still rejected without changing choice
- [x] Locked-only validation branch and locked-only presentation/layout tests removed; playable validation (playable class must carry actives) still enforced

## Comments

Landed: Ranger/Mage definitions plus the registry locked branch deleted; presentation
locked branches, tooltip output, `NOTICE` kind and veil removed; command suggestions
follow `all()` so they are playable-only with no code change. `ClassDefinition.locked`
and the `PlayerClasses` locked check stay as dead-safe defense.
