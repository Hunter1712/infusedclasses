# Spec: Pretty Talent Tree — Borderlands Spine, Longer Defence Progression, Uncapped Levels

Status: ready-for-agent

## Problem Statement

The talent panel is a flat vertical list of word-buttons: no tree read, no branch identity, no reason to look at it twice. The Juggernaut path is also too short — a levelled player finishes the whole build early and sits on a pile of dead points with nothing to spend them on, and the hard level cap makes further play feel pointless.

## Solution

Turn the talent panel into a card-per-tree picker in the same visual language as the class-select screen, with the Juggernaut card holding a Borderlands-style vertical spine: alternating blocks of small defence stat nodes and big passive/upgrade nodes, connected by state-coloured lines, clicked by icon instead of word-buttons. Lengthen Juggernaut with defence-only filler nodes on staggered spend gates so progression lasts, collapse the War Cry fork down to the single Resistance upgrade, add one tank capstone that deepens the existing Thick Skin engine, shrink per-node stat values for balance, and remove the hard level cap with a flattened post-20 XP cost so play keeps earning.

## User Stories

1. As a Warrior, I want one card per tree with its own accent colour, so that Juggernaut, Berserker, and Warlord read as distinct identities.
2. As a player, I want locked trees dimmed with a coming-soon read, so that I know more is planned without being teased by clickable buttons.
3. As a Juggernaut, I want my tree to read top-to-bottom like Borderlands tiers, so that I always know what unlocks next.
4. As a player, I want small stat nodes between big passive nodes, so that every level gives a small step toward a big payoff.
5. As a player, I want nodes as icons with connecting lines instead of full-width word-buttons, so that the screen looks like a tree instead of a settings list.
6. As a player, I want small nodes visually smaller than big passive/upgrade nodes, so that keystones stand out at a glance.
7. As a player, I want connecting lines coloured by the child node's state, so that I can trace what is unlocked, available, or still gated.
8. As a player, I want node state from border and dimming (unlocked with check, available bright, gated dimmed), so that I never have to read a paragraph to know what I can click.
9. As a player, I want full name, blurb, cost, and reject reason in the hover tooltip, so that icons stay clean while detail stays one hover away.
10. As a player, I want clicking an available icon to spend points through server validation, so that clients cannot cheat unlocks.
11. As a player, I want the level, XP-to-next, and unspent-points header inside the Juggernaut card, so that economy state sits where I spend.
12. As a player, I want a respec button on the panel that refunds spent points, so that I can re-path without slash commands.
13. As a Juggernaut, I want defence-only stats on every filler node, so that the tank fantasy never dilutes into damage or speed.
14. As a player, I want stat blocks of four nodes feeding one passive, repeating down the spine, so that progression alternates earn-and-payoff.
15. As a player, I want staggered spend gates down the spine, so that skipping stat blocks straight to the capstone is impossible.
16. As a Juggernaut, I want reduced per-node stat values, so that a full build feels strong but not godmode against vanilla gear tiers.
17. As a Juggernaut, I want Thick Skin mid-spine that grants armor stacks when hit, so that taking hits makes me tougher.
18. As a Juggernaut, I want a single War Cry upgrade that adds Resistance with longer duration, so that my one active has one clear build moment.
19. As a Juggernaut, I want a capstone that makes Thick Skin endure longer with a higher cap, so that my endgame reward deepens the playstyle I already learned.
20. As a player, I want no hard level cap with post-20 XP costs flattening to a fixed ceiling, so that long play keeps earning points for the longer spine.
21. As a player, I want my unlocks, XP, level, and points to survive death, relog, and respawn, so that the longer grind never wipes.
22. As a player, I want denied picks to explain why in tooltip and chat, so that gates teach instead of frustrating.
23. As a player, I want level-ups to still toast and sound, so that the uncapped climb keeps feeling rewarding.
24. As a server admin, I want unlocks and respec validated server-side with command fallbacks, so that testing and recovery stay frictionless.
25. As a player on a small window, I want the taller card to still fit without scrolling in MVP, so that pretty does not break usability.

## Implementation Decisions

- Presentation reuses the class-select vanilla-plus idiom: dimmed backdrop, card fill, top accent strip per tree, 1px border that lightens on hover, hover wash, dim overlay for locked cards. No new UI framework.
- Card roster is three cards in one row: Juggernaut active with the icon spine inside, Berserker and Warlord locked placeholders with their own accent colours and a coming-soon read.
- Branch colours: Juggernaut iron-red, Berserker burnt orange, Warlord royal purple; locked cards forced grey. Node state overlays branch colour: unlocked gets check plus green read, available bright, gated/no-points greyed with reason in tooltip.
- Node rendering is vanilla-only: item-icon rendering and built-in GUI sprites for health, armor, shield, horn, and strength/resistance motifs, with a plain colour-block fallback. No binary texture assets are added.
- Interaction keeps the existing server-authoritative intent pattern: icon bounds are the click bounds, tooltips carry name plus blurb plus cost plus gate reason, the client sends spend/respec intents and the server validates class, points, gates, and single-path rules before re-sending state.
- Spine shape is two inner stat columns converging into Thick Skin, continuing through a second stat block into the single War Cry upgrade, then a final stat block into the Enduring Skin capstone. Lines render beneath icons as thin fills.
- Node economy: stat nodes cost one point each and reuse the existing flat passive modifier path; the two big upgrades use the special (non-attribute) path like the current conditional and active upgrade. Total full-build spend lands in the mid-teens so a maxed legacy character still has a couple of spare points.
- Filler stat pool is defence-only: max health, armor, toughness, knockback resistance. No attack, speed, or new attribute plumbing.
- Balance trims per-node values down from the current baseline (health halved, armor thinned to every other tier, toughness fractional, knock resistance small flats) so the longer chain totals roughly a vanilla-plus defensive set rather than a full gear tier.
- War Cry is a single upgrade, not a fork: the potency variant is removed along with its mutual-exclusion rule, leaving duration plus Resistance as the one upgrade.
- Enduring Skin capstone only reparametrizes the existing Thick Skin engine (higher stack cap, longer out-of-combat decay). No new trigger, tick, listener, or condition system.
- Level curve keeps the existing exponential shape through level 20 and flattens to a fixed ceiling per level beyond, so post-20 progression stays earnable instead of scaling into unreachable kill counts. One point per level is unchanged.
- Persistence shape is unchanged (XP, level, unspent, unlocked ids); removed or renamed nodes are dropped on decode, unlocks otherwise carry over death and relog; stacks stay in-memory only with decay and wipe on death.
- Fixed-fit layout for MVP: the taller card targets common window heights with no scroll or pan/zoom; narrow-window overflow handling is deferred.

## Testing Decisions

- A good test asserts external behavior of pure progression logic, never widget pixels: XP cost per level including the post-20 flatten ceiling, points granted per level with no cap, gate rejections at each spine tier, respec refund equalling points spent, Thick Skin cap and decay including the Enduring Skin parameters, single War Cry upgrade adding Resistance with the longer duration, serialization round-trip of progress with removed-node filtering, unlocks surviving a respawn copy.
- Seams: one seam — the pure tree/progression unit (XP curve, node data, gates, spend, respec, stack machine, active-upgrade effects, persistence codec). No new seams; the capstone is covered by reparametrizing the existing stack seam, and the curve flatten by extending the existing XP seam. GUI, keybind wiring, particles, and sound stay manual-only.
- Prior art: plain JUnit tests for registry math, passive values, cooldown state machine, and class state transitions; same style, no gametest harness.
- Manual verification in dev client only for the panel: fresh, mid-spend, and full-unlock states at two window sizes, screenshots for icon read, line read, tooltip content, denied-pick messaging, header counts, respec refund, and locked-card dimming. Level-up toast and sound confirmed live.

## Out of Scope

- Berserker and Warlord trees beyond locked cards; Ranger and Mage trees; new HUD elements, rage bars, or cooldown widgets; pannable/zoomable canvas or scrolling; custom art, icon PNGs, fonts, or datapack JSON extensibility; attack or mobility filler stats; new trigger or condition engines; survival respec costs; permanent stacks; per-branch (as opposed to total-spent) gate tracking; balance tuning beyond the Juggernaut baseline shrink; automated GUI tests.

## Further Notes

- References: Borderlands for tight tiered branches with spend gates, the small-filler-plus-keystone model for the small-versus-big node split.
- Grilling trail: hybrid card-plus-tree read, vanilla-only icons with fallback, staggered total-spent gates to enforce order, defence-only pool, single Resistance upgrade after fork cut, Enduring Skin over anchor/shout variants for zero new systems, flatten-ceiling uncapped curve over true exponential tail.
- If the single progression seam feels wrong (e.g. you want the XP curve or the stack machine tested in isolation from tree data), say so — the spec assumes one seam on purpose.
