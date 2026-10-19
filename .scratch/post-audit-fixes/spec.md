# Spec: Post-audit fixes

Status: implemented — in-game: armor/crit read, one-blow feedback, particles, rhythm follow-ups and the spear thrust seen; the raised-shield zero-earn check still owed (needs a second player)

## Problem Statement

A follow-up audit found six things worth fixing. The stance and crit mults settle
against a base the damage hook reports before armor, so armor prices the bonus a
second time while the under-1.0 branch heals a pre-armor amount: on iron armor a
x1.25 crit reads x1.19 and Scutum plus Testudo's x0.85 take reads x0.75, and the
balance model (which states the mults as post-mitigation) cannot see it. The
`lastHurt` memory is written as if the bonus always landed, so a rejected bonus
inflates the window. 1.21.11's own `Player.crit` is empty, so mod crits show no
particles while two javadocs promise them. Every cooldown, window and grace rides
`System.currentTimeMillis()`. `PROJECT_DESCRIPTION.md` still sells the alpha-era
kits and numbers. `ServerEvents` (247 lines) is over the repo's own ~150-line line
and mixes wiring, press routing and the ordered hit settle.

## Solution

Make the mults truly post-mitigation, restore the crit particle cue, put the
server clock on a monotonic source, refresh the project description, and split
`ServerEvents` into wiring plus two adapters.

- The settle scales what landed. A new `HitMitigation` mirrors
  `LivingEntity.getDamageAfterArmorAbsorb` + `getDamageAfterMagicAbsorb`
  (same order, same tag gates, same `CombatRules` helpers) without their side
  effects — the armor-durability hit and the resistance stats belong to the real
  hit once. The bonus hurt rides `infusedclasses:settle`, whose tag files put it
  in `bypasses_armor`, `bypasses_effects` and `bypasses_shield`, so mitigation is
  paid exactly once. The under-1.0 branch heals the landed share.
- `DamageSettle.hurtGuarded` restores `lastHurt` by the health delta the bonus
  actually took off, so a rejected or fully eaten bonus adds nothing.
- `CritCue` spawns `ParticleTypes.CRIT` at the victim (the ring for a crit, the
  crack for a betrayal unchanged); the dead `attacker.crit(target)` call goes.
- `InfusedClasses.now()` returns monotonic millis and replaces the six server
  `System.currentTimeMillis()` call sites. Diffs only; client HUD elapsed stays
  client-local.
- `PROJECT_DESCRIPTION.md` rewritten to current Beta copy.
- `ServerEvents` keeps lifecycle wiring and the steady-state emission point;
  `MeleeHook` takes the AFTER_DAMAGE body, `SlotPresses` takes the press arms
  (ADR-0009's explicit-switch shape kept, home moved).

## Implementation Decisions

- The damage type is a datapack entry (`data/infusedclasses/damage_type/settle.json`)
  plus three additive tag files; no registry code. `CritCue` and `HitMitigation`
  stay adapters.
- The balance model needs no change: it already prices the crit post-mitigation
  (`REF_CRIT` multiplies outside `dr`, whose penetration reads the base damage),
  so only the code moved to match it.
- The crit cue's particle call is the mod's own because the vanilla method is a
  no-op in this version; the sound path is unchanged.
- The clock contract is unchanged — `PlayerRuntime` still takes `now` as a
  parameter; only the entry's source changes.
- No new seams beyond `HitMitigation`; no gametest harness (per the prior
  round's decision — adapters are verified in game).

## Testing Decisions

- `DamageSettleTest` keeps pinning the pure curve and gains the post-mitigation
  base cases (landed 6.0 at x1.25 adds 1.5, at x0.85 heals 0.9 back).
- `HitMitigation`, the damage-type registration, the particle call, the clock
  source and the `ServerEvents` split are verified by reading and in game:
  no game objects in JUnit, same as today.
- The full suite stays green; `tools/mixin-targets.sh` resolves after the
  javadoc-only mixin touch.

## Out of Scope

- The audit's other notes already closed elsewhere; no re-tuning of balance
  knobs.
- The client HUD's countdown anchoring (ADR-0017/0021).
- Client `StatusStore` elapsed clocks (client-local diffs, no server clock).

## Further Notes

- Audit mapping: findings 1 (settle vs armor), 2 (project description),
  3 (`lastHurt` memory), 4 (crit particles), 5 (wall clock), 6 (`ServerEvents`
  size).
- In-game hand-checks (AGENTS.md) still owed for the settle: hit plus bonus
  health drop, one sound/flash/knockback, later hits compare against the
  combined total.

## Comments

Implemented on the working tree. `HitMitigation` is the new landed-math adapter:
`DamageSettle` converts `damageTaken` to what landed, settles the combined mult in
one step, rides the new datapack damage type (`data/infusedclasses/damage_type/settle.json`
plus the three additive `data/minecraft/tags/damage_type` files — no registry code:
`BuiltInRegistries.DAMAGE_TYPE` does not exist in 1.21.11, damage types are a datapack
registry) and restores `lastHurt` by the health delta the bonus actually took off.
The guarded hurt calls `hurtServer` with the target's `ServerLevel` directly: vanilla's
`hurt` is a deprecated final delegate to exactly that call, and the direct form is the
one the mixin contracts already describe.

`ServerEvents` drops to 100 lines: lifecycle wiring, receivers and the tick fan-out.
`MeleeHook` (103) owns the AFTER_DAMAGE body and `settleAttackerHit`; `SlotPresses`
(79) owns the press prologue and both explicit arms (ADR-0009's status line notes the
home moved, shape unchanged). `InfusedClasses.now()` serves all six server call sites;
the client HUD keeps its local `currentTimeMillis`. `CritCue` spawns
`ParticleTypes.CRIT` at the victim beside the crit sound.

The balance model needed no change: it already prices the crit post-mitigation
(`REF_CRIT` multiplies outside `dr`, whose penetration reads the base damage), so the
implementation moved to match it.

Full suite green (255 tests, 1 added); `tools/mixin-targets.sh` resolves 6 targets;
`tools/doc-flags.sh` and `tools/arch-flags.sh` clean. In-game (1.21.11 client, log
clean of mod errors): a Gambler's iron sword vs a full-iron target read 3.2 normal
(= 3.12 landed) and 4.7 crit (= 3.12 x 1.5) — the old double-mitigated code would
have read 4.5, so the fix is seen in game. One sound/flash per hit, crit particles
back, and repeated normal-rhythm hits steadied at 3.2 (the `lastHurt` memory holds
the combined total). The full-charge spear thrust was seen on 2026-10-19 (log clean). Still
owed per AGENTS.md before the round is done: the raised-shield zero-earn contract (needs a
second player to raise the shield).

Round review (uncommitted, both axes) fixed five findings in place: the ADR index
line for 0009 now records the switch's home, `MeleePlan`'s gate value is named
`damageTaken` and its docs no longer read "landed" as the settled base, ADR-0012's
consequences describe the post-mitigation crit pricing, the spec status flipped to
implemented, and the settle type pays no second hunger exhaustion (`exhaustion: 0.0`).
Accepted as-is: `SlotPresses`' repeated announce lines (ADR-0009 mandates the explicit
arms), `HitMitigation` reaching into `living` (a deliberate vanilla mirror), its
single-caller shape, and the `(player, def, runtime, now)` clump (ADR-0017 requires the
explicit `now`).
