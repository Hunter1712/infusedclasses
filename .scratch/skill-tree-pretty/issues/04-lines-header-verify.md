# 04: Connector lines, header, respec, and verification pass

**What to build:** The finished tree read on top of the card panel: thin connector lines under the icons coloured by child state tracing the spine top-to-bottom, the level/XP/points header inside the Juggernaut card, and a respec button refunding spent points through the existing server path — closed by the full manual verification matrix.

**Blocked by:** 01, 03 (needs uncapped economy plus the rendered panel).

**Status:** ready-for-agent

- [ ] Connector lines trace every spine edge beneath icons and take the child node's state colour
- [ ] In-card header shows level, XP-to-next (flattened past 20), and unspent points with no stale max-cap read
- [ ] Respec button refunds spent points and re-applies base state without commands
- [ ] Denied picks, level-up toast/sound, and locked-card dimming confirmed live
- [ ] Screenshot matrix captured: fresh, mid-spend, and full-unlock states at two window sizes
- [ ] Full pure-logic suite green with no new texture assets in the repo
