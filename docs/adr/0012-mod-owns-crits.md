# ADR-0012: The mod owns crits and chance enters decisions as data

Status: accepted

## Context

Vanilla crits are an on-demand ×1.5 for any player who times a falling swing:
outside the class system, identical for every class, and sitting directly next
to where a chance-based class would put its identity. The roster also has no
vocabulary for a passed-in random roll: every existing decision reads
definitions and transient state, never chance. Peers (Juggernaut, Legionary)
were tuned before any crit system existed, so a universal crit is a patch on
their numbers, not part of them.

## Decision

1. **Retire vanilla crits for every player.** The mixin cancels
   `Player.canCriticalAttack` — the single 1.21.11 gate that turns on the
   ×1.5 damage, the `PLAYER_ATTACK_CRIT` sound and the crit particles. Mobs
   are untouched, and `magicCrit`/ENCHANTED_HIT stays (it is the
   enchanted-hit sparkle, not a crit).
2. **Replace them with a universal full-charge crit: 10% at ×1.25, for every
   player, classless included.** ×1.25 rather than vanilla's ×1.5 keeps
   tuned-without-crits peers near their power and leaves the spike to the
   class built on it.
3. **A class can override the crit rule, not the system.** One `CritRule`
   record per class (chance, mult, betrayal, Streak step); the Gambler's dice
   are the first. `CritRoll` is the pure decision over either rule, and the
   mixin holds no per-class policy.
4. **Chance enters decisions as passed-in data.** The server entry draws one
   roll per landed full-charge hit and passes it to the plan; no decision
   module touches a random source, so boundaries are assertable in plain
   JUnit.
5. **Crits settle as the swing's deal mult through the existing second-hurt
   settle**, and the verdict drives the feedback: a crit replays the vanilla
   cue, a betrayal gets a distinct one.

## Consequences

- Every player loses on-demand jump crits and gains a small random spike;
  average full-swing damage for peers moves about +2.5%.
- Crits become learnable, documented state (glossary: crit roll, betrayal,
  Streak) instead of an undocumented vanilla quirk; copy and docs must not
  promise jump crits again.
- The second-hurt settle means armor and enchantment reduction apply to the
  crit part too (near-linear, so the net reads as base times mult), and the
  bonus is dropped when the base hit already killed. Betrayal's heal-back is
  pre-armor, so it lands shallower against armored targets.
- A future class can declare its own crit identity with data alone; a class
  that wants vanilla jump crits back would reintroduce a policy seam, which
  was deleted here on purpose.
