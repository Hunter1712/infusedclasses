# ADR-0005: Hit gating, stack storage, and trigger-scan seams

Status: accepted

## Context

Three seams sat in the wrong place after the Juggernaut inversion:

1. `ServerEvents` gated the whole melee-hit branch on
   `activeAbilities().contains(RUSH_ABILITY)`: an ability-id comparison in the
   server entry deciding whether Momentum builds. A future class with an
   `on_hit_dealt` stacker but no Rush would silently earn nothing, and the
   pure modules it routes to already gate themselves (`onTrigger` is inert on
   unknown triggers, the Rush bonus no-ops without a live window, the Rend
   spend returns empty without one).
2. `PlayerRuntime` (194 lines) owned two jobs: stack storage (counts, grace
   timestamps, decay, caps) plus cooldown lanes and ability windows, with four
   rush verbs, four rend verbs and six stack verbs on one interface.
3. The trigger-found lookup lived on `RendAbility` (`momentumOf`), so the Rush
   bonus path (`grantRushBonus`) resolved its priced passive through the Rend
   decision — the wrong owner for a shared priced passive.

## Decision

1. **The entry never compares ability ids to gate rewards.** The damage branch runs for any
   attacker with a class; gating stays in the pure modules. Classless attackers
   still cost no transient state (the runtime is looked up only behind the
   non-null check, as before). Bind-morph routing is the one carve-out: the intent
   receivers may read the current class's ability ids only to pick which decision
   module to call (Legionary `R` to Rotate, `G` to Impetus/Testudo per stance);
   decisions and applies stay pure/adapter (see ADR-0006).
2. **Stack storage moves to `PassiveStacks`.** Counts, grace timestamps, bump,
   decay, and clear live there; `PlayerRuntime` keeps its public verbs and
   delegates, coordinating stacks with cooldowns and windows. Callers and tests
   still cross the runtime seam, so the existing suites assert unchanged
   behaviour through it.
3. **The class owns the trigger scan.** `ClassDefinition.passiveForTrigger`
   is the single scan; `RendAbility.momentumOf` delegates to it as the
   decision's semantic alias (its tests still pin it), and the passive applier
   resolves spend and bonus through the definition directly.

## Consequences

- A second `on_hit_dealt` stacker works without touching the entry, the
   applier, or either ability decision — one registry entry, as the deepening
   spec wants.
- Stack bugs have one home (`PassiveStacks`, 88 lines); window and cooldown
   coordination stays in `PlayerRuntime` (194 down to 158 lines).
- The cost: one more module to know about, and `momentumOf` reads as a
   duplicate until its alias comment is read. If a second trigger-found lookup
   appears, revisit whether the alias still earns its keep.
- Both the runtime and the applier still sit at or just over the ~150-line
  review trigger, but both pass the deletion test: delete the runtime and
  lane-plus-window arming scatters across the entry, the damage hook and the
  switch path; delete the applier and the write-only-when-different rule
  scatters across tick, switch and spend. If either grows again, the next
  split is the attribute read/write mechanics at the bottom of
  `PassiveApplier`.
