# 01: HUD row decision

**What to build:** Pure decide-then-apply decision for one effect-like row: given a `ConditionalPassive`, its stacks, grace/decay timing, and now, return icon id, title, subline, seconds-left, bar fraction, and hidden flag. No `DrawContext`, player, or clock inside.

**Blocked by:** None.

**Status:** done

- [x] Stacks N maps to amplifier title `Bulwark <ROMAN>`: `1` to `I`, `4` to `IV`, `9` to `IX`, `10` to `X`; whole-number armor formatting like picker copy (`1`, not `1.0`).
- [x] Subline is `+N armor` from `stacks * perStack` for Bulwark (`minecraft:armor` reads as `armor`).
- [x] Icon is `minecraft:resistance`; row carries it as an id, never loads a texture.
- [x] Seconds-left plus bar fraction drain the passive's own `graceSeconds` (hit resets, then past grace every stack is gone); overdue time reads as zero.
- [x] Hidden flag is true when `stacks <= 0`; multi-row ordering helper lists only `stacks > 0` passives in class-registry order.
- [x] JUnit at the decision seam only: Roman edges, title/subline text, grace-only seconds/bar at grace start, mid-grace, and grace end, hidden and ordering cases.

## Answer

`PassiveHud` (row decision: `rowFor`, `roman`, `visiblePassives`, `visibleInRegistryOrder`) plus `PassiveHudTest` (6 tests). `gradle test` green.
