# 01: Passive effect as data, stacks-to-attribute as a pure decision

**What to build:** A conditional passive carries the facts needed to apply its own effect — which attribute it grants, what its modifier is called, and what each stack contributes. A pure module answers one question: given this passive and its current stack count, what modifier (if any) should be on the player. The server entry applies that answer to the attribute. The entry class no longer contains the passive's name as a literal, and no longer compares a passive's name to a string to find the effect.

From the player's side nothing changes: Bulwark still grants one armor per stack, capped at ten, decaying one per second after five seconds without a hit. What changes is that the numbers live with the passive instead of in the mod entry class, and that the mapping can be asserted without a server.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] A passive definition carries which attribute it affects, its modifier identifier, and its per-stack contribution.
- [x] A pure module maps a passive plus a stack count to the modifier to apply; zero or fewer stacks yields no modifier.
- [x] The mapping is assertable in plain JUnit with no player, level, attribute instance, or graphics object constructed.
- [x] The mapping is covered at zero stacks, one stack, and above the cap.
- [x] The server entry reaches the effect through the passive definition, with no name comparison against a literal anywhere in it.
- [x] The passive's player-visible description is either generated from those fields or stops asserting numbers, so copy cannot drift from behaviour.
- [x] The attribute write is still idempotent: the modifier is only touched when the target value differs from what is already applied.
- [x] Falling to zero stacks removes the modifier.
- [x] All existing stack, decay, grace-period and persistence tests pass unchanged.
- [x] Bulwark's gameplay numbers are unchanged: one armor per stack, ten stack cap, five second grace, one stack per second after.