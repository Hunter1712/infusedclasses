# 03: Card panel with icon nodes and states

**What to build:** A card-per-tree talent panel in the class-select visual language: Juggernaut as the active card holding the spine as small/big icon slots instead of word-buttons, Berserker and Warlord as dimmed locked cards with their own accent colours. Node state reads from border and dimming with full detail in hover tooltips, and clicking an available icon spends points through the existing server-validated intent path.

**Blocked by:** 02 (needs the final spine node list to lay out).

**Status:** wontfix

## Comments

SUPERSEDED — implemented in `1fbfc1c`/`e7f03d5`, then the design was replaced by the ranked BL2-style panel in `skill-tree-ranks` (generic grid renderer, corner badges, banner, name plates; Warlord card cut). Do not implement from this file.

- [ ] Three cards render in one row with per-tree accent strips; locked cards are dimmed with a coming-soon read and no clickable nodes
- [ ] Small stat nodes render smaller than big passive/upgrade nodes; all nodes use vanilla-only icons with a colour-block fallback and zero new texture assets
- [ ] Unlocked, available, and gated states are visually distinct with tooltip carrying name, blurb, cost, and reject reason
- [ ] Clicking an available icon spends via server validation; denied picks explain why in tooltip and chat
- [ ] Manual dev-client screenshots at two window sizes, no automated GUI test
