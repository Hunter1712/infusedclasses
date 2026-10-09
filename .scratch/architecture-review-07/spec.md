# Architecture review round 7: seven deepenings, lazy cuts

Status: done

## Problem statement

The review (report in OS temp dir, 2026-10-09) found seven seams left by round
6: the hit settle's name lying about bump-plus-return, the card init chaining
through a dummy model beside an orphaned public verb, two liveness readers
disagreeing with the production peeks after spend, one trigger scan behind
three names, the stack gate enforced twice with a literal joining them, the
Rend fire/consume pairing pinned nowhere JUnit can reach, and empty-case
guards mirrored on both sides of the settle seams.

Prior art respected: oath dispatch stays in the entry (ADR-0009), the lane map
stays hidden (ADR-0008), lane/window forwards stay (rounds 4+6), fire-time
consume vs hit-time spend stay separate events (round 4), Rotate bare announce
stays (round 4).

## Solution

Shortest diff per candidate, deletion over addition:

1. `settleHitOneShots` renamed `settleHit`: settles a planned hit, returns the
   stamped Rend bonus. Callers (`ServerEvents`, two `MeleePlanTest` rows) and
   glossary follow. Shipped.
2. Screen `init` resolves `cardTextFor` directly (byte-identical, chain gone).
   Test rewrite and privatization skipped: the parts stay pinned by
   `CardDeepeningTest`/`ClassPresentationTest`, and churning them buys no
   behavior. Shipped half.
3. `isRushLive`/`isRendEmpowerLive` deleted; ability tests re-pinned through
   the production verbs (`hasRushBonusPending`, `peekRendEmpowerStacks`).
   Glossary cooldown line follows. Shipped.
4. `pricedPassive` wrapper deleted; consume and grant resolve
   `RendAbility.spentPassive` visibly. Shipped.
5. `HitPlan` carries the gated `charge`; the settle replays it through the
   shared gate instead of a `1.0F` literal. New `MeleePlanTest` row proves a
   lying spam plan bumps nothing. Shipped.
6. Spender consume split: pure `dropStacks` (JUnit-reachable) plus the
   attribute removal in `consumeSpent`, which routes through it. New
   `RendAbilityTest` row witnesses fire-then-drop: stacks drop once, second
   drop finds nothing, the armed snapshot survives fire-time. Entry path
   unchanged. Shipped.
7. Entry-side empty-case pre-checks deleted (`rendHp`/`impetus > 0`,
   `take != 1.0`); the adapter no-ops (`dealBonus`, `settleMobTake`) own the
   guards next to the math. Victim lazy-pair untouched (load-bearing).
   Shipped.

GLOSSARY.md moves with the responsibilities (passive applier, cooldown
windows, card text, picker screen). Full suite green (140 tests, 0 failures).
