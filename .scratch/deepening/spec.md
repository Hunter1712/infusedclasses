# Spec: Deepen Class-System Modules

Status: ready-for-agent

## Problem Statement

The class system works for Warrior plus locked placeholders, but its knowledge is scattered. A maintainer adding Ranger or Mage must touch the server entry, the per-player state, the registry, and the picker screen in parallel, with no single place to verify the change. War Cry mixes class gating, cooldown, effects, sound, particles, and chat in one static flow. Class validation is fused with live-player effects so lock-rejection and strip-before-apply have no unit seam. Picker text duplicates registry knowledge in parallel switches on class id. Packet types live on the server entry so the client imports across the wrong seam. Attribute names are resolved by two overlapping switches. Each item is small alone, but together they make the next class expensive and easy to drift.

## Solution

Deepen the shallow modules without changing player-visible behavior. War Cry becomes an ability module with a small result interface behind which gating and cooldown hide. Per-player class state splits into a pure validation module plus a modifier-effect adapter. Class definitions carry their own presentation data so the picker renders generically. Packet types move onto their payloads or a single protocol hub. Attribute resolution collapses to one seam. Coming-soon shape becomes explicit and Warrior tuning numbers become named. Warrior stats, War Cry effect and cooldown, first-join picker flow, persistence across relogs, and the admin command all behave exactly as before.

## User Stories

1. As a maintainer, I want War Cry gating and cooldown decided in one ability module, so that I copy one pattern for the next ability.
2. As a maintainer, I want a War Cry result of fired, wrong class, or on cooldown with remaining time, so that callers map outcomes without reimplementing checks.
3. As a maintainer, I want class validation separate from live-player effects, so that I test rejection without a game harness.
4. As a maintainer, I want unknown, missing, and locked class ids rejected through one pure path, so that picker, command, and persistence agree.
5. As a maintainer, I want class switching to strip old modifiers before applying new ones in one place, so that buffs never stack.
6. As a maintainer, I want class presentation data owned by the class definition, so that adding a class is adding one definition.
7. As a maintainer, I want the picker screen to render passives and abilities generically, so that no per-class switch needs updating.
8. As a maintainer, I want accent color, tagline, and ability text asserted through the registry, so that screen drift is caught by tests.
9. As a maintainer, I want packet types owned by their payloads or a protocol hub, so that the client never imports the server entry.
10. As a maintainer, I want the open, pick, and confirm round-trip readable as one protocol, so that a stuck picker is impossible to miss.
11. As a maintainer, I want one attribute resolution seam for both effects and display, so that unknown attributes fail in one module.
12. As a maintainer, I want class registration to fail fast on unknown attributes, so that typos surface at startup not on equip.
13. As a maintainer, I want coming-soon distinct from playable with no passives, so that a locked class can never be selectable by shape.
14. As a maintainer, I want Warrior tuning numbers named, so that balance intent is traceable without reading a static block.
15. As a player, I want class selection, War Cry timing, cooldown feedback, and persistence to behave identically after deepening, so that I notice nothing.
16. As a Warrior, I want War Cry to fire only when I am a Warrior and off cooldown, so that the ability feels unchanged.
17. As a Warrior, I want remaining-seconds feedback when War Cry is on cooldown, so that I know when it is ready.
18. As a new player, I want the picker on first join and silent load on rejoin, so that first-join detection is unchanged.
19. As a server admin, I want the class set and select commands to validate and apply in one place, so that unknown or locked classes still fail cleanly.

## Implementation Decisions

- A War Cry ability module owns class gating by active ability (not by hardcoded class id), cooldown transition, and tuning constants, exposing a small result interface of fired, wrong class, or on cooldown with remaining millis. The server entry thins to mapping that result to effects, sound, particles, and chat.
- Per-player class state splits into a pure validation module (try-set by id, refresh current) plus a modifier-effect adapter behind a seam (apply and remove for a definition). The mixin owner slot stays as the persistence seam.
- Class definitions carry presentation alongside rules: accent color, tagline, ability title, and ability blurb. The picker screen keeps only a generic passive-list and ability renderer plus layout and card framing.
- Networking packet types live on their payload records, or on a single protocol hub in the common package if direction grouping reads better. Both server and client entries only register; neither imports the other entry.
- The open, pick, and confirm round-trip is documented as one protocol: open on first join, pick as client intent, confirm as server verdict that alone closes the picker.
- Attribute handling collapses to one module owning both gameplay resolution and display naming from a single table, with fail-fast registration when a passive names an unknown attribute. The per-player state accepts the resolver rather than hardcoding it.
- Class shape distinguishes playable (passives plus ability) from coming-soon (no ability, no passives) so locked-but-selectable and unlocked-but-blank are impossible by construction.
- Warrior tuning values become named constants tied to the design intent of health bonus, armor bonus, toughness bonus, and speed penalty, preserving current numbers exactly.
- Server authority is unchanged: client sends intents only, every decision happens on the server, cooldowns reset on relog unless a future exploit forces persistence.
- Confirmed test seams are the three pure interfaces: ability result, class-state validation, and definition data via the registry. Adapter seams for live modifiers and vanilla attributes exist for production but are not new test surfaces in this spec.

## Testing Decisions

- A good test asserts external behavior through the module interface: given a class id the validation accepts or rejects; given a cooldown window the ability fires, rejects inside, and accepts at the edge with correct remaining time; given the registry the Warrior numbers, lock flags, and presentation data match intent; switching definitions strips prior modifiers.
- Tested modules are the War Cry ability result transitions, the pure per-player class-state validation including lock and unknown rejection, and the class registry definitions including numbers, lock invariants, and presentation data.
- Prior art is the existing plain JUnit suites with no game bootstrap and no gametest harness, asserting cooldown math, passive math, lock flags, and id string round-trips. That style is kept and extended; no new harness is introduced.
- Not tested through automated suites: rendering, GUI layout, keybind plumbing, sound and particle emission, mixin internals, live attribute application. Those paths rest on unchanged behavior plus manual verification in a dev client.
- The thin effect-mapping code in the server entry and the generic picker renderer are intentionally left without dedicated unit seams; correctness rests on the pure modules they delegate to.

## Out of Scope

- New playable classes beyond Warrior, new abilities, War Cry radius or team behavior, taunt, dash, or slam.
- Balance tuning of Warrior numbers beyond naming them.
- Cooldown persistence across relogs beyond the accepted reset.
- Merging payload records into fewer network channels; per-channel types stay as the platform requires.
- Config files, datapack extensibility, class kits or items, custom GUI art or assets.
- A new test harness, gametest setup, or coverage for rendering, GUI, keybinds, or mixins.

## Further Notes

- Source for this spec is the architecture review of 2026-10-04 covering six deepening candidates, with the War Cry ability module plus pure class state as the top recommendation. The review HTML lives outside the repo in the OS temp directory.
- Deletion-test survivors deliberately kept shallow and untouched: the cooldown value object, the four payload records, the mixin owner slot, and the definition and passive records. Removing them would scatter complexity.
- No domain glossary or decision records exist yet in the repo; this spec uses the class-system spec vocabulary of class, Warrior, War Cry, picker, passives, and cooldown, and introduces no new domain terms.
- The class-system spec contradiction between the early melee-bonus story and the later no-flat-damage decision stands resolved toward implemented behavior: no flat bonus, War Cry as the damage tool.
