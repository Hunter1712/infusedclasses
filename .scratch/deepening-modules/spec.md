# Deepening the mod's modules

Status: done

## Problem Statement

The mod works, but the places where the game is touched are shallow modules: the mod entry class holds payload registration, event handlers, packet receivers, commands, the armor effect for one passive, and the whole War Cry blast radius; the per-player state class holds a pure validation path and a live stack path with no seam between them; the picker screen crosses layout, frame, text and widget layers and re-derives meaning from rendered strings.

The cost is concentrated in the code that changes most. Conditional passives and picker presentation account for the recent churn, and those are exactly the areas where a change means editing the entry class or the screen. Adding a second armor passive, a second ability, or a second stacked bar all mean copy-paste into a method that already does too much.

An agent reading this repo also has to bounce across seven files to understand class selection and five to understand the passive stacks. There is no vocabulary recorded for these concepts either: no glossary, no ADRs.

From the player's side nothing is broken today. This spec is about the maintainer's and the agent's ability to change class behaviour without touching unrelated modules, and about being able to test decisions without a running game.

## Solution

Adopt one rule across the mod: **decide purely, apply in an adapter.** Every gameplay or presentation decision becomes a pure function of data that returns a description of what should happen; the server entry, the mixin, or the screen applies it. Three existing places already follow this rule (cooldown state, ability trigger, card lines) — this spec extends it to the passive effect and to card styling rather than inventing a seam per candidate.

Then split the per-player state class along the seam it already draws implicitly, so the choice-and-persist half has no clock in it and the transient stack half owns its own.

Finally, record the domain vocabulary so the next reader does not have to infer it.

## User Stories

1. As a maintainer, I want the passive effect expressed as data on the passive, so that adding an armor-granting passive is a registry entry instead of an edit to the mod entry class.
2. As a maintainer, I want the mapping from passive stacks to a player attribute to be a pure decision, so that I can assert the mapping without a server, a player, or an attribute instance.
3. As a maintainer, I want the mod entry class to be a wiring module, so that it registers callbacks and delegates, and nothing else.
4. As a maintainer, I want the mod entry class back under the architecture-review trigger, so that the trigger in the project's agent instructions keeps meaning something.
5. As a maintainer, I want one rule for how effects are applied rather than one per effect, so that the next effect follows a shape I already recognise.
6. As a maintainer, I want the ability result to carry what should happen to the player, so that the entry class maps one result to one outcome instead of hardcoding it.
7. As a maintainer, I want a second ability to be a definition plus an adapter, so that adding it does not copy the War Cry method.
8. As a maintainer, I want the stack-to-armor mapping to live with the passive definition, so that the passive name is not looked up as a string in two places.
9. As a maintainer, I want card lines to carry their own style, so that the screen stops comparing rendered strings to decide how to draw them.
10. As a maintainer, I want the card drawing rules in one module, so that a picker redesign does not disturb widget wiring.
11. As a maintainer, I want the picker screen to own only widgets and hover, so that it reads as a screen rather than a drawing program.
12. As a maintainer, I want the per-player choice state to have no clock reads in it, so that it is obviously pure.
13. As a maintainer, I want transient stacks in their own module with an injected clock, so that decay and grace-period behaviour is testable without waiting.
14. As a maintainer, I want the entry class to own the lifetime of transient per-player state, so that respawn and logout handling live in one place.
15. As a maintainer, I want the respawn copy to stop copying transient state, so that the copy operation has one meaning.
16. As a maintainer, I want the single-method accessor interface to either earn its place or disappear, so that it is not a seam in name only.
17. As a maintainer, I want class lookup indirected only when a second source of classes appears, so that I do not pay for a hypothetical seam.
18. As a maintainer, I want the deferred lookup seam recorded as a decision, so that a future review does not re-propose it as if it were new.
19. As an agent, I want the domain vocabulary in a glossary, so that I use the project's words rather than inventing my own.
20. As an agent, I want each of these decisions recorded as an ADR, so that I do not re-litigate them.
21. As an agent, I want to know which seam tests belong behind, so that I test behaviour rather than wiring.
22. As an agent, I want one seam to test through instead of three, so that the test suite stays a description of behaviour.
23. As an agent, I want existing tests to keep passing without being rewritten wholesale, so that the refactor is provably behaviour-preserving.
24. As an agent, I want the pure decision to be reachable without constructing game types, so that tests run in plain JUnit.
25. As a player, I want Bulwark to grant exactly one armor per stack, capped at ten stacks, so that nothing changes about how the class plays.
26. As a player, I want stacks to decay one per second after five seconds without a hit, so that nothing changes about how the class plays.
27. As a player, I want War Cry to still give Strength for eight seconds on a thirty-second cooldown, so that nothing changes about how the class plays.
28. As a player, I want the picker to look and read exactly as it does now, so that nothing changes about how the game looks.
29. As a player, I want a locked class to stay unselectable, so that nothing changes about progression.
30. As a player, I want my class to survive relog and respawn, so that nothing changes about persistence.
31. As a maintainer, I want each candidate shippable independently, so that a refactor I do not agree with does not block the ones I do.
32. As a maintainer, I want the entry class to shrink as each candidate lands, so that progress is visible.

## Implementation Decisions

- **One seam, stated once.** The rule is: a pure decision module takes data and returns a description of an outcome; an adapter applies it. The mod already has three instances of this rule and this spec does not add a fourth rule.

- **The seam is drawn at the highest point that stays pure.** Pure decisions must not require constructing a player, a level, an attribute instance, or a graphics object. Anything that needs one of those belongs in the adapter.

- **Passives carry their effect.** A passive definition gains the facts needed to apply its effect: which attribute, what modifier identifier, and what each stack contributes. The per-stack value and the modifier identifier are facts about the passive, not constants in the entry class. The existing free-text description already documents these numbers, so the description and the behaviour stop being independent.

- **Stack-to-attribute mapping is a pure decision.** A pure module answers: given a passive and its current stack count, should a modifier be applied, and at what value. A zero or absent stack count yields no modifier. The decision returns the description; the server entry applies it to the attribute.

- **The passive is addressed by its definition, not by its name.** The current tick walks the class's passives comparing a name literal against a lookup string. After the change the effect is reached through the passive definition. There is no name comparison in the server entry.

- **Attribute writes are idempotent and re-applied on change.** The existing behaviour of only writing when the target value differs from the current modifier is kept. The adapter owns removal when stacks fall to zero.

- **Ability result carries the outcome.** The existing ability trigger result gains whatever the adapter needs to apply the ability, instead of the entry class hardcoding effect, sound, particles and chat. The three existing result kinds are unchanged; wrong class and on cooldown still short-circuit before anything is applied.

- **The on-cooldown message is a decision.** Formatting the remaining seconds is part of what the result decides, not something the entry class formats. The rounding and the locale belong with the decision so it can be asserted.

- **The passive effect, the ability effect and the card style are three instances of one seam, not three seams.** The passive effect yields an attribute description; the ability effect yields an outcome description; the card style yields a colour and weight per line. Each is pure; each has exactly one adapter.

- **Card lines carry their style.** A rendered card line gains the colour and weight the screen needs, derived from its line kind. The special-case comparison against a rendered notice string disappears from the screen, because the line's kind is what the screen already switches on.

- **The "coming soon" notice stops being a magic string.** The notice is identified by its line kind, not by matching text.

- **Card drawing is one module per card.** A card module owns the frame palette, the accent strip, the locked veil, the hover highlight and the per-line styling. The screen owns widget construction, hover position and the layout maths.

- **The picker screen keeps exactly what a screen is for.** It builds the buttons from the presentation data, tracks which card is hovered, and forwards the pick as an intent. It does not decide colours.

- **Per-player state splits into two modules at the seam it already draws.** One module owns the chosen class, validation, and the NBT round-trip; it has no clock reads and no transient state. The other owns stacks, the cap, decay and the grace period, with the clock injected.

- **Nothing transient crosses the seam.** Copying player state on respawn copies the choice only. The transient half starts empty on the new player, as it does today.

- **The transient half's lifetime belongs to the server entry.** Since it is no longer reachable from the player, the server entry holds it per player and owns creation, respawn handling and teardown. One place decides how long transient state lives.

- **The single-method accessor interface is kept only if it earns its place.** It is a seam in name only today: one method over a large implementation, introduced for the mixin cast. If the choice half ends up reachable through it, keep it and narrow it to what the mixin genuinely needs. If not, delete it and have the mixin expose the state directly. Deletion is the default outcome.

- **Cooldowns stay as they are.** They are already a deep module over a trivial state, and the ability work does not need to touch them.

- **The class lookup seam is deferred, not built.** One adapter exists today, which is a hypothetical seam. Introduce the lookup interface at the moment a second source of classes appears, not before. Until then, callers reach the registry directly.

- **Candidate ordering is a dependency, not a preference.** The passive effect module lands first because it removes the attribute writes from the tick; that shrinks what remains in the state class before the split. The split lands second. Ability execution and card rendering are independent of both and can land in any order or in parallel. The deferred lookup seam lands last, and only on its trigger.

- **Each candidate ships as its own change with its own verification.** No combined refactor commit. Client-visible changes ship by deploying the jar, never on compile alone.

- **The mixin keeps its persistence hooks.** The NBT key and its round-trip behaviour are unchanged; only the module behind them moves.

- **Presentation text stays in the presentation module.** No copy is taken on "fixing" card wording as part of this work; visible text must not change.

- **Glossary and ADRs.** The deepened modules are named after concepts that do not yet exist in the project's domain docs. Create the glossary and record the ADRs for the decide-then-apply rule, the deferred lookup seam, and the transient-state lifetime as part of this work.

## Testing Decisions

- **A good test asserts behaviour through the public interface of one module and knows nothing about how the result is applied.** If a test has to construct a player, a level, an attribute instance, or a graphics object to make an assertion, it is testing the wrong side of the seam.

- **Test the pure decisions, in plain JUnit, with no game bootstrap.** The cooldown tests are the prior art: a state machine asserted directly with injected time. The stack tests are the other prior art: they already use the injectable-clock overloads, so they move across the split with their assertions unchanged.

- **One seam to test through, three instances of it.** The stack-to-attribute mapping, the ability outcome and the card line styling each get asserted as pure decisions. There is no fourth new test surface.

- **The existing suite is the behaviour contract.** All current tests must pass after the refactor. Proof of behaviour preservation is that the assertion set is unchanged: the same facts about stacks, cooldowns, ability gating, card lines and tooltips are still asserted, just against modules that moved.

- **Assertions move with the code, they do not get rewritten.** Where a test targets a module that changes shape, rewrite the import and the call site only. A test whose expectation changes needs the change justified in the spec's implementation decisions, and there is currently no such case.

- **New assertions cover what is newly pure.** Add coverage for the stack-to-attribute mapping at zero, at one, and above the cap; for the ability outcome's effect duration, amplifier and message formatting; and for card line styling across every line kind including the notice.

- **Negative cases stay covered.** Unknown triggers stay inert, unknown and locked class identifiers stay rejected, a rejected pick leaves the previous choice untouched, transient state stays out of persistence and out of the respawn copy, and a locked class stays unselectable.

- **The protocol tests are unaffected.** Channel identity and codec round-trips do not move.

- **Presentation tests assert text and style together.** Card-line tests gain style assertions alongside the existing text and kind assertions, because style is now part of what a line decides.

- **No new test for wiring.** Registration of callbacks and receivers is wiring, not behaviour, and stays uncovered by design.

## Out of Scope

- The deferred class lookup seam. Recorded, not built.
- Any new class beyond the three that exist. Locked classes stay locked with no ability and no passives.
- Any change to gameplay numbers: cooldown length, effect duration, amplifier, stack cap, per-stack armor value, or the grace period.
- Any change to visible text, card layout, colours, or the picker's look and feel.
- Any change to the client-server protocol: no new payloads, no new channels, no codec changes.
- Any change to the NBT key or to what persists.
- Per-frame or per-tick performance work beyond keeping the existing rule of only writing an attribute when the value actually changes.
- Replacing the manual GUI drawing with vanilla widgets. The current screen draws its own cards deliberately; that is a separate piece of work if it is ever wanted.
- Domain docs beyond the glossary entries and ADRs this work implies.

## Further Notes

- The strongest argument for this ordering is that candidate 1 makes candidate 2 cheap. Once the attribute writes leave the server tick, the remaining pure/live split in the per-player state class is small enough to move in one pass, and the shallow-split pattern named in the project's agent instructions can be retired rather than left as standing guidance.

- The highest-risk part is not the module moves, it is preserving the exactly-once attribute write. The existing rule of only writing when the value differs must survive the move, or the attribute modifier will be recreated every tick and any eventual attribute recalculation will compound it.

- Several of the strings currently in use are free text that also documents behaviour, notably the passive description. Care is needed so that moving the numbers into real fields does not leave the description asserting numbers that can now drift from the behaviour. Either the description is generated from the fields or it stops stating numbers.

- The tests currently reach into two modules for the same fact (class choice and registry lookup). That overlap is not a bug to fix here, but it means test churn is expected in the state class and registry when candidate 2 lands.

- The three shallow signals worth watching after this work: the mod entry class line count, whether any new name comparison appears in the server entry, and whether the screen ever needs to know what a line says in order to draw it.