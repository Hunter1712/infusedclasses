Status: ready-for-agent

## Problem Statement

Warrior currently ships with flat attribute passives: +8 max health, +4 armor, +2 toughness, and -10% movement speed. Those stats are static and always-on. The user wants the stat-passive system removed and replaced with reactive conditional passives that matter during combat, not a permanent character sheet.

## Solution

Delete the old flat-stat passive pipeline entirely. Introduce a new conditional-passive model: each passive is event-triggered, accumulates up to a cap, decays after a grace period, and live-applies an attribute bonus proportional to current stacks. Start with one Warrior passive, "Bulwark": when Warrior takes damage, gain 1 armor stack; stacks apply +1 armor each up to 10; 5 seconds without a new hit drops every stack at once.

## User Stories

1. As a player who picks Warrior, I want my armor bonus to respond to incoming hits, so that I feel rewarded for staying in a fight.
2. As a player browsing the class picker, I want to see Warrior's conditional passive described, so that I know what it does before choosing.
3. As a player who avoids conflict, I want my bonus armor to fade when nothing has hit me, so that my stats stay honest.
4. As a player who rejoins with Warrior already selected, I want my class to survive NBT saves, so that conditional passives keep working across relogs.
5. As a developer adding a new conditional trigger later, I want the trigger to be a string keyed helper, so that I can add behaviors without replacing the whole data model.
6. As a developer reading the codebase, I want the old flat-stat modifier pipeline gone, so that I do not have a dead parallel system to reason about.
7. As a player looking at a class card, I want the passive name and a short description, so that the card stays compact.
8. As a player wanting the full rule text, I want to hover the class card for a tooltip, so that I can read the detailed bonus and cap.
9. As a player selecting a locked class, I want to see "COMING SOON", so that the picker does not promise features that are not there.
10. As a QA tester, I want pure unit tests around stack accumulation, decay, and armor bonus math, so that regressions are caught without a server.

## Implementation Decisions

- Deep-clean removal: `Passive`, `ClassAttributes` (and its display/formatting helpers), `ClassApplier`, `ServerModifiers`, the `ModifierSink` seam, and passive-specific tests are deleted. `ClassDefinition` drops its `List<Passive>` field.
- Replacement: `ClassDefinition` gains `List<ConditionalPassive> passives`. Record shape: `ConditionalPassive(String name, String trigger, int maxStacks, ResourceKey<Attribute> attribute, Identifier modifierId, double perStack, int graceSeconds)` with `shortDesc()` and `fullDesc()` generated from those numbers, so the copy cannot drift from behaviour.
- Warrior's flat +8 health, +4 armor, +2 toughness, -10% speed are gone. Warrior becomes active-ability-only plus one conditional passive.
- Warrior conditional passive is named `Bulwark`, trigger string `on_hit_taken`, effect `+1 armor` per stack, max 10 stacks. Decay rule: more than 5 seconds without a hit drops every stack at once.
- Trigger evaluation is modeled around open-ended `String` keys. Only `on_hit_taken` is implemented now; unknown triggers are inert and ignored.
- Stack state is transient per-player and held alongside the player's selected class state, not persisted to NBT.
- The bonus is applied by a small helper that replaces the previous modifier on each stack change: value becomes `stacks * 1.0` on `minecraft:armor` with `ADD_VALUE`. This is a live attribute edit, not a persistent class stat.
- `PlayerMixin` holds the choice slot per player and the NBT read/write of the selected class. The server entry owns one transient runtime per player plus the hurt hook and tick path that feed the conditional passive pipeline.
- Locked classes: picker still shows `Passives: ???`, `Active: ???`, `COMING SOON`. Unlocked: each passive renders as its name then short description; missing passives render `No passives yet`.
- Picker subtitle becomes "Your choice shapes your ability". Tooltip joins passive full-description text the same way ability lines are joined today.
- UI render path treats passive lines distinctly from ability lines, no longer relying on `ClassAttributes.display`.

## Testing Decisions

- Delete `ModifierSinkTest`, `ClassAttributesTest`, and all `Passive` assertions in existing tests. Keep and adapt `ClassRegistryTest`, `ClassPresentationTest`, `ClassAttributesTest` replacements that assert on `ConditionalPassive` data and the pure stack/decay/bonus math.
- Prefer pure tests at the highest seam: how many stacks apply the passive, how decay ticks, and how armor bonus is derived. Hook validation may use a small in-memory sink or a pure helper result; avoid live-player tests where the same rule can be asserted from the helper output.
- Class presentation tests assert card lines and tooltips for locked and unlocked classes, including empty passives.

## Out of Scope

- Implementing more than the `on_hit_taken` trigger.
- Persisting conditional-passive stack counts across relogs.
- Adding multiple new classes or abilities.
- Balancing values beyond the one defined Warrior passive; further balance work is a product decision later.

## Further Notes

- This spec assumes no backwards-compatibility shims for the old `Passive` type; any saved references should have been removed as part of the deep clean.
- `AGENTS.md` and source javadoc should stop referencing `Passive`, `ClassApplier`, and passive-specific comments after the clean.
