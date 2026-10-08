# Architecture review round 2: six deepenings, lazy cuts

Status: done

## Problem statement

The review (report in OS temp dir, 2026-10-08) found six shallow or leaking seams:
13 ability-id comparisons across eight modules, a 20-method transient-runtime
facade, an oath switch that re-decides the morph, untestable settle math plus a
recomputed Rend curve, a card model resolved then dismantled, and picker copy
formatted by a module that does not own the numbers.

## Solution

Shortest diff per candidate, deletion over addition:

1. `ClassDefinition.hasAbility` absorbs all 13 checks; both morph checks key on
   `ROTATE`. Shipped. (`pricedPassive` skipped: `passiveForTrigger` already owns
   the scan.)
2. `armOath(now, boolean)` takes the `Window` instead. Shipped. (`Lane` enum,
   consume-vocabulary fusion, forwarder deletion skipped: churn against pinned
   tests and widened seams.)
3. Morph dispatch stays in the entry per ADR-0009 (dispatch would drag the player
   across the decide-then-apply seam). Routing fix ships via 1.
4. Pure `DamageSettle.settleMath` pinned by `DamageSettleTest`; the hook consumes
   `plan.rendHp()` instead of recomputing the curve. Shipped. (Settle unification
   skipped: the two settles already delegate; the entry guard avoids adapter work.)
5. `CardModel` carries display name plus tagline; both draw calls take the model.
   Shipped. (Hover-state caching skipped: no measured cost.)
6. Earn/idle chains fuse into one `phrasing()` table. Shipped. (Per-ability
   `pickerDesc`, registry copy move skipped: no drift has bitten.)

## Verification

`gradle test`: 136 tests, 0 failures. `gradle deployClient`: success.
