Status: superseded — effect-row HUD deleted; vanilla armor bar is the Bulwark display, no stacks signal.

## Problem Statement

Bulwark stacks are invisible during combat. Stacks live only in the server transient runtime and reach the player as a `minecraft:armor` modifier (`infusedclasses:bulwark_armor`); the client learns nothing about count or decay. The player cannot tell whether they hold 1 or 10 stacks, or when grace runs out.

## Solution

Add a client HUD overlay that mimics the vanilla status-effect row and shows each earned conditional passive while its stacks are above zero. Ship with Bulwark only: resistance effect icon plus `Bulwark <ROMAN>` (stacks N renders as level N, so 3 stacks reads `Bulwark III`), a `+N armor` subline, and a vanilla-like bar plus `Ns` countdown covering grace into decay. Server stays authoritative and pushes stack changes; the client interpolates the countdown between messages.

## User Stories

1. As a Warrior taking hits, I want to see my Bulwark stacks as an effect-like row, so that I know my current bonus without opening any screen.
2. As a Warrior mid-fight, I want to see how long until my stacks start decaying, so that I can stay in the fight to keep them.
3. As a Warrior with War Cry active, I want the Bulwark row to sit below the vanilla effect icons, so that Strength and Bulwark never cover each other.
4. As a player with zero stacks or no class, I want no Bulwark row at all, so that my screen stays clean when the passive is inert.
5. As a developer adding a second conditional passive later, I want the row renderer and sync to be per-passive, so that the next passive reuses the same HUD path.
6. As a QA tester, I want the row content, amplifier text, visibility, and countdown math asserted as pure decisions, so that regressions are caught without a running client.

## Implementation Decisions

- Decide-then-apply split: a pure HUD-row decision (given passive definition, stacks, and now, return icon id, title line, subline, seconds-left, bar fraction, hidden flag) plus a thin client adapter that draws whatever the decision returns. The decision never touches `DrawContext`, player, or clock; the adapter owns all of those.
- Sync is a new server-to-client signal (same family as the picker signal and pick confirm): sent only when a passive's stack count changes (hit bump, decay tick that moved, class switch clear, respawn clear), carrying per-passive entries of passive name, stacks, per-stack armor value, grace seconds, and millis-until-decay-starts at send time. The client stores the last entry per passive and ticks the displayed countdown down locally.
- Row content for Bulwark: icon is the vanilla `minecraft:resistance` effect sprite (effects group only, no item sprites, no new texture for v1); title is `Bulwark <ROMAN>` with stacks N mapped to level N (`1` to `I`, …, `10` to `X`); subline is `+N armor` from `stacks * perStack` formatted like picker copy (whole numbers as `1`, not `1.0`).
- Countdown is the grace left, vanilla-style: seconds-left drains the passive's own `graceSeconds` and resets on every hit; past grace every stack is already gone. The bar fraction drains that same grace, so copy cannot drift from behaviour.
- Placement is inside the vanilla effect HUD, not beside it: each passive's icon joins the beneficial strip in vanilla's own 24px slots, and over inventory screens a vanilla-metric box (`Name <ROMAN>`, `mm:ss`) appends below vanilla's list rows. Like vanilla, nothing draws over screens that hide effects.
- Server hook points reuse the existing ones: the hit trigger path, the server-tick decay path (`tickDecayMoved` set), and the class-switch path (`onClassChanged`) are the only places that may emit the signal. Classless players emit nothing and cost no packet.
- No real `MobEffect` is registered: the row never appears in the inventory effect list, never interacts with milk/clear, and never changes the armor attribute path (`PassiveEffect` plus passive applier stay the sole writer of `infusedclasses:bulwark_armor`).

## Testing Decisions

- Pure JUnit at the decision seam: stacks-to-Roman (`0` hidden, `1` to `I`, `4` to `IV`, `9` to `IX`, `10` to `X`), title/subline text, hidden flag (zero stacks, no class, `hideGui`), grace-only seconds-left and bar fraction at grace start, mid-grace, and grace end, exact millis drop boundary, and multi-passive ordering in registry order.
- Sync unit test: entries are emitted only when the moved set is non-empty plus on class change/clear, and carry the millis-until-decay value the client interpolates from.
- No live-client draw tests; the adapter draws verbatim whatever the decision returns.
- Stack, decay, and applier tests were rewritten for the all-at-once drop and stay green with it.

## Out of Scope

- Custom icon texture or resource-pack art; icon config screen.
- Showing exact per-second decay ticks as numbers beyond the combined bar plus seconds.
- Persisting stacks across relog (stacks stay transient per the transient runtime rule).
- Real `MobEffect` registration or inventory effect-list integration.
- Rebalancing Bulwark numbers (max 10, +1.0 per stack, 5s grace stand).

## Further Notes

- If the resistance sprite lookup drifts on 1.21.11 input/tooltip changes, fall back to text-only row for v1 and keep the sprite as a follow-up; row layout must not depend on sprite size.
- Follow-up candidates: custom Bulwark sprite, per-row fade-out animation on reaching zero, config for corner/offset.
