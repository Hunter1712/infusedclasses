# 04: The strip on screen (draw, corner, hiding)

Spec: `.scratch/status-hud/spec.md`

**What to build:** The demoable feature: the slot strip itself. One HUD element renders the rows decision verbatim — uniform badge boxes in vanilla-slot style with no backdrop, labels on the inner side mirrored per corner, the strip growing up from its bottom corner. It hides on F1, while any screen is open, and for classless players; `hud.enabled=false` stops the drawing and nothing else; `hud.corner` moves it.

**Blocked by:** 02 (the rows), 03 (the snapshot and store), `.scratch/player-tools` 01 (the config file it reads).

**Status:** ready-for-agent

- [ ] In-game pass behind `gradle deployClient` + `/class select`, per class: lanes dim/lit with one-decimal countdowns ticking; the stride rider appears, `+` drops after a full swing while the rider keeps counting; Testudo's 1.5s chip appears and expires; empower and IMP riders vanish when spent; Momentum shows count and grace; Streak appears above zero; a Zero spin shows no chip; a class switch wipes the strip; respawn and relog re-seed it.
- [ ] F1 and any screen hide it; classless players see nothing; `hud.enabled=false` draws nothing while play is unchanged.
- [ ] Both corners place and mirror correctly after a config edit and restart; no overlap with chat or the hotbar.
- [ ] Live instance jar refreshed via `gradle deployClient`; `gradle test` still green.

## Comments

None yet.
