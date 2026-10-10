# Architecture review round 8: seven deepenings, lazy cuts

Status: done

## Problem statement

The review (report in OS temp dir, 2026-10-10) found seven seams left by round
7: the stride tick riding outside the reconcile home its own docstring claims,
the stance remover rebuilding the aura lists a third time, the card wrap
rebuilding the line join beside its owner, the flip record keeping millis
fields beside the tick-clocked window family, intent routing living in a
second entry module, the card model resolved twice per frame, and the mob-take
guard duplicated between wrapper and core.

Prior art respected: oath dispatch + Rend consume stay in the entry (ADR-0009),
no Rend adapter (ADR-0008.2), card parts stay pinned (ADR-0008.5, round 7),
lane/window forwards stay (rounds 4+6), hit ordering stays entry-applied
(ADR-0008.4).

## Solution

Shortest diff per candidate, deletion over addition:

1. `PassiveApplier.apply` owns the stride tick (`RushApplier.tick` call moves
   in from the entry tick); entry tick crosses one seam. Docstring order
   follows. Shipped.
2. `StanceApplier.removeStanceModifiers` iterates a private `allModifiers`
   home over the same aura/window lists. Shipped.
3. `wrapRows` takes the `CardLine` and derives the name/desc split from
   `line.text()`: the join literal lives in one place. Shipped.
4. Flip record (`flipAt`/`impetusPending`) becomes `flipWindow` on the window
   family; `Window` speaks millis throughout (tick windows convert `* 50L` at
   their call sites). Public verbs byte-identical: pending is only read under
   a Gladius landing. Shipped.
5. `registerReceivers` moves from `InfusedClasses` to `ServerEvents`; the
   initializer keeps payload registration and delegates. Oath switch moves
   home unchanged. Shipped.
6. Screen resolves `cardFor` once per frame (background pass caches in
   `frameModel`, body pass reuses with fallback). Shipped.
7. `settleMobTake` keeps only the null guards for source resolution; alive and
   1.0 guards ride in `settleWithSource`. Shipped.

GLOSSARY.md untouched: no term changed hands (stride still laid by its own
adapter, tick order never enumerated, windows still transient). Full suite
green (143 tests, 0 failures).
