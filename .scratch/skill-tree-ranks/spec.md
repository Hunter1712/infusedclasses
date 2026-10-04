# Spec: Ranked Borderlands Talent Trees (Defence First)

Status: ready-for-agent

## Problem Statement

The talent panel is fifteen one-shot nodes gated by invisible total-spent numbers: the spine overflows small windows, gate thresholds are magic integers that must be renumbered by hand whenever a node moves, layout and icons are hardcoded per node id in the screen, and the cut potency fork left a single upgrade with no visible relationship to the base ability. The locked Warlord card has no design left behind it now that ally auras are out of scope. Level-by-level progression feels like checking boxes off a list, not climbing a Borderlands tree.

## Solution

Rebuild the Juggernaut Defence tree Borderlands-style and prove the pattern once: ranked stat lines with pips instead of a dozen one-shot nodes, explicit tier rows with visible point labels, the base War Cry shown as an owned action-skill header, upgrades attached to the nodes they modify, and one flashy capstone at the bottom. Unlock logic becomes tier spend plus a short keystone chain instead of magic gate numbers. Old saves migrate forward without wipes. The Berserker Offence tree is designed on paper in this spec and built in a follow-up; it needs no new systems beyond a kill hook that already exists for XP and one cooldown-reduce method.

## User Stories

1. As a Warrior, I want one card for Defence and one locked card for Offence, so that the two fantasies read as distinct identities.
2. As a player, I want the base War Cry shown as an owned action-skill header, so that I see what the upgrades modify.
3. As a player, I want stat lines with ranks and pips instead of a dozen one-shot nodes, so that the tree fits on screen and each click feels like growth.
4. As a player, I want visible tier rows with point labels, so that the unlock rule is on screen instead of hidden in tooltips.
5. As a Juggernaut, I want Thick Skin available in the first tier, so that the tank identity lands at level 2.
6. As a player, I want Hold the Line in the second tier with a visible edge to the War Cry header, so that the augment relationship is obvious.
7. As a Juggernaut, I want Enduring Skin attached to Thick Skin as an upgrade ring, so that deepening the engine reads as deepening, not as a separate destination.
8. As a Juggernaut, I want Rallying Roar as the single capstone, so that the bottom of the tree changes the button I already press.
9. As a player, I want the keystone chain enforced (no War Cry augment without Thick Skin), so that skipping the fantasy to rush numbers is impossible.
10. As a player, I want free choice among stat ranks around the forced spine, so that two Juggernauts can differ.
11. As a player, I want each rank to cost one point, so that every level is one click.
12. As a player, I want slower leveling than today with no level cap, so that a 20-point tree is a whole-playthrough grind that never hard-stops.
13. As a player with an old save, I want my progress folded into ranks on load, so that the redesign never wipes me.
14. As a player, I want removed node ids to keep dropping silently, so that old cuts stay cut.
15. As a player, I want denied picks to explain why in tooltip and chat, so that gates teach instead of frustrating.
16. As a player, I want a respec button refunding everything, so that re-pathing never needs slash commands.
17. As a player, I want the tree to fit without scrolling on common windows, so that pretty never breaks usability.
18. As a player, I want unlocked, available, and gated states distinct at a glance with detail one hover away, so that I never read a paragraph to know what I can click.
19. As a player, I want level-ups to still toast and sound, so that the longer climb keeps feeling rewarding.
20. As a player, I want my unlocks, XP, level, and points to survive death, relog, and respawn with stacks still wiped on death, so that the grind never wipes.
21. As a player on a server, I want unlocks and respec validated server-side with command fallbacks, so that clients cannot cheat and testing stays frictionless.
22. As a server admin, I want the same class and talent commands working unchanged, so that recovery flows survive the redesign.
23. As a future Berserker player, I want the Offence card visibly locked with its fantasy intact, so that I know what is coming.
24. As a maintainer, I want tier thresholds and parent edges as data, not magic integers and id switches in the screen, so that adding a node touches one record.
25. As a maintainer, I want one pure-logic seam covering ranks, tiers, chain, codec, and effects, so that the whole tree is assertable without a game harness.

## Implementation Decisions

- Branch structure is two cards: Juggernaut Defence (built) and Berserker Offence (locked placeholder). Warlord is cut; it has no capstone left now that ally auras are out.
- Defence ships twenty points: Vigor at five ranks of max health, Bulwark at five ranks alternating armor on odd ranks and toughness on even ranks, Hide at five ranks of knockback resistance (all one point per rank flat), Thick Skin as the tier-one keystone, Hold the Line as the tier-two War Cry augment, Enduring Skin as the tier-three upgrade ring on Thick Skin, Rallying Roar as the one-point capstone granting Absorption and a shorter War Cry cooldown. Thin stat values stay near today's: roughly plus five health, three armor, one toughness, and a quarter knock resistance for a full build.
- Tier thresholds count points spent in the tree at zero, five, ten, and twelve, with the capstone additionally requiring Hold the Line. While Defence is the only tree, the global spent count is identical to per-tree spend; dedicated per-tree tracking lands with Offence.
- Parent edges exist only for attachments: Enduring needs Thick Skin, Hold the Line needs Thick Skin, Rallying Roar needs Hold the Line. All other gating is tier spend. The rejected alternative (literal tier-clear) would have forced identical builds and rendered respec meaningless.
- Unlock checks return a small result type (ok, need-points, missing-parents-or-tier, already-maxed) from pure logic; the screen formats tooltips and the server formats chat at the edges, replacing the current English-string reason.
- Progress stores ranks per node id instead of an unlocked set; respec refunds rank-times-cost. The save shape is otherwise unchanged (experience, level, unspent, entries). Legacy entries migrate on decode: bare ids count as rank one, old vigor, plate, ward, and hide ids fold into the matching ranked line capped at five, removed and unknown ids keep dropping.
- Experience keeps the exponential curve through level twenty with a flat per-level ceiling beyond and one point per level; only the base cost rises by roughly half, stretching time-to-clear without touching the point budget.
- The War Cry header is presentation only: it renders owned for Warriors and carries no spend, no state, and no unlock rule. Hold the Line draws an edge up to it.
- Rallying Roar reparametrizes the existing shout path (one more effect plus a cooldown value owned by unlocks); cooldowns still reset on relog as the accepted lazy default.
- The panel renders generically from node grid coordinates: two-column stat slots, centered keystones, attached rings offset beside their base, tier dividers with point labels, rank pips with count read, a distinct capstone frame, and state borders with dimming plus check marks. Icons stay vanilla-only with the existing color-block fallback, and no new texture assets are added. The layout targets common window heights with no scroll.
- Offence stays data-only in this spec: an attack-damage line, an attack-speed line, a ranked Warmonger kill line, and Fuel the Fire as capstone, reusing the existing kill hook and one new cooldown-reduce method at build time. Its numbers are explicitly marked to-be-balanced then.
- Full-build versus rush math is preserved as the balance anchor: cheapest capstone path near thirteen points against twenty total, roughly a quarter of points as genuine choice.

## Testing Decisions

- A good test asserts external behavior of the pure tree unit, never widget pixels: rank accrual to max and rejection beyond, Bulwark parity values per rank, tier rejection below spend thresholds, keystone-chain rejection (augment without Thick Skin), respec refund equaling rank-times-cost, legacy save folding with caps, unknown-id drops, uncapped high-level round-trips, Rallying effects plus cooldown value, and the thirteen-point rush path.
- Seams: one seam, the existing pure tree and progression unit, extended to ranks, tiers, chain, codec migration, result type, and active-upgrade effects. No new seams; the capstone is covered by the existing effect path, the cooldown change by the existing cooldown seam. GUI, keybind wiring, particles, and sound stay manual-only.
- Prior art: plain JUnit suites for registry math, cooldown state machine, and class state transitions; same style, no gametest harness.
- Manual verification in dev client only: fresh, mid-spend, and full-unlock states at two window sizes with screenshots for tier read, pip read, tooltip content, denied-pick messaging, header counts, respec refund, locked-card dimming, and no-scroll fit. Level-up toast and sound, death and relog persistence with stack wipe, and the testing-command fast paths confirmed live.

## Out of Scope

- Building the Offence tree beyond locked-card placeholder and paper data, including per-tree spend tracking, the kill-stack hookup, and the cooldown-reduce method.
- Taunt or pull abilities, ally auras or radius buffs, a third branch, escalating rank costs, per-node respec refunds, level-gated tiers, cross-node synergies.
- New HUD elements, scrolling or pannable canvas, custom art, icon files, fonts, datapack extensibility.
- Balance tuning beyond the stated baselines; Hide's knock-resistance total carries a playtest flag.
- Automated GUI tests or a new test harness.

## Further Notes

- Grilling trail: ranked lines over many one-shots (Borderlands 5-of-5 convention, one maxed line per tier step), attached upgrade rings over spine stops (Dragon Age diamond layout), tier-spend plus keystone chain over literal tier-clear (full-clear would force identical builds and void respec), merged Bulwark with alternating ranks over split armor and toughness lines, one-point capstone back on the twenty-point budget, per-tree spend deferred to Offence, slower curve via one base-cost constant.
- References: Borderlands 2 for tier rows, rank pips, and capstones; Dragon Age Inquisition for ability headers with attached upgrades; the small-filler-plus-keystone model for the stat-versus-moment split.
- The white-line read from the last screenshot review (a line segment not matching its child state) is assumed fixed by rendering edges explicitly instead of guessing connectivity; call it out if it survives.
