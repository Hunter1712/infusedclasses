# Architecture review round 6: seven deepenings, lazy cuts

Status: done

## Problem statement

The review (report in OS temp dir, 2026-10-09) found seven remaining seams after
five deepening rounds: the Rend spend agreed across five modules with tripled
empty-hit guards, the transient runtime's fourteen near-identical lane/window
verbs hiding one-shot vs persistent semantics, the hit sequence ordered by
convention in the entry lambda, the card resolved three ways with the frame
taking parts against the glossary, stance tradeoff numbers existing under two
names each, outcome landing spelled three ways, and the per-tick reconcile
order living in one method body under a passive adapter's name.

Prior art respected: round 4 put peek/consume pairing in `settleHitOneShots`
but left the land value and the bump outside it (fire-time stack consume and
hit-time snapshot spend stay separate events by design); round 4 ruled lane
forwards load-bearing under ADR-0008; ADR-0009 keeps oath dispatch in the
entry (untouched here).

## Solution

Shortest diff per candidate, deletion over addition:

1. `PassiveApplier.settleHitOneShots` returns the stamped Rend bonus to land
   and owns the bump too: bump, Rush credit, Rend spend in field order, land
   only what it spent. `ServerEvents` lands the return instead of
   `plan.rendHp()`. Pinned by two `MeleePlanTest` rows (landed equals the
   stamped bonus; empty hit lands 0). Shipped.
2. Runtime verbs stay (abilities plus tests cross them per round 4 and
   ADR-0008); one-shot (Rush/Rend: plan peeks, settle spends) vs persistent
   (Impetus/Testudo: peeked every hit, never consumed) named on the windows
   and the liveness readers. Documented, no deletion.
3. Attacker branch extracted to `ServerEvents.settleAttackerHit`: plan,
   settle, Rend bonus, Impetus bonus, stance settle in one named home, so the
   hook cannot reorder or half-apply a hit. Shipped.
4. `ClassCardRenderer.drawFrame` takes the `CardModel`; both screen passes
   resolve `cardFor` once per card (`init` takes `.text()` from
   `cardFor(def, false)`); stale "cached per definition" comment fixed.
   `cardTextFor`/`cardStyleFor` stay public as the seams tests pin. Shipped.
5. `IMPETUS_WINDOW_TAKE_FACTOR` / `TESTUDO_WINDOW_DEAL_FACTOR` deleted;
   `MeleePlan` reads `IMPETUS_TAKEN_MULT` / `TESTUDO_DEAL_MULT` like the picker
   copy. `RotateAbility.ROTATE_MS` alias deleted; copy and tests read
   `Cooldowns.ROTATE_MS`. `LegionaryTest` pins the single sources. Shipped.
6. Every fired-or-fizzle outcome lands through `AbilityApplier.announceAndCast`:
   `RushApplier` does cleanse+Speed when fired then one shared call; Rotate
   passes no sound (bare announce by design, per round 4). Shipped.
7. `PassiveApplier.apply` javadoc names the single reconcile home (decay,
   conditionals, HP-scaled pair, stance aura; switch mirrors in
   `onClassChanged`). Single caller, single switch path: no further split.
   Documented, no new module.

GLOSSARY.md moves with the responsibilities (ability applier, cooldown
windows, melee plan, Rend, passive applier, card text, picker screen, card
renderer). Full suite green (138 tests, 0 failures).
