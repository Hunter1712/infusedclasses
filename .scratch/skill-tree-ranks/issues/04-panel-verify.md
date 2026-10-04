# 04: Generic panel and verification pass

**What to build:** The panel rendered generically from node grid coordinates: two Defence cards (Juggernaut active, Berserker dimmed locked placeholder), tier dividers with point labels, rank pips with count read, attached rings offset beside their base, a distinct capstone frame, state borders with dimming plus checks, full tooltip content, the in-card level/XP/points header, and the respec button through the existing intent. Closed by the full manual verification matrix.

**Blocked by:** 01, 02 (needs the final node data and rules; Rallying live-check additionally needs 03).

**Status:** ready-for-agent

- [ ] Seven slots plus header read top-to-bottom with no scroll on common windows; small and big slots visually distinct
- [ ] Tier dividers, corner rank badges, action-skill banner, and name plates render; tooltips carry name, rank, cost, and reject reason
- [ ] Respec refunds through the server path; denied picks, toast/sound, and locked-card dimming confirmed live
- [ ] Screenshot matrix captured: fresh, mid-spend, and full-unlock at two window sizes; death and relog keep progress with stacks wiped
- [ ] Full pure-logic suite green with no new texture assets in the repo
