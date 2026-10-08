# Architecture review round 4: seven deepenings, lazy cuts

Status: done

## Problem statement

The review (report in OS temp dir, 2026-10-08) found seven shallow or leaking
seams: melee peek/consume ordered across the entry, the Rend spend agreed in
three places, the runtime re-exporting every cooldown lane, one-use holder
lookups behind a shared adapter, a Vigor tick duplicating the Plan shape plus a
Legionary marker in the passive adapter, a card model resolved three times per
card, and stance write/remove lists with no shared source.

## Solution

Shortest diff per candidate, deletion over addition:

1. `PassiveApplier.settleHitOneShots` owns the peek/consume pairing in field
   order with `planHit`; the entry routes the plan. Pinned by two
   `MeleePlanTest` rows. Shipped.
2. `RendAbility.spentPassive` single-sources the priced resolution; the hook
   side funnels through the settle in 1. (Fire-time stack consume and hit-time
   snapshot spend stay separate events by design.) Shipped.
3. Lane forwards are load-bearing (abilities plus tests cross them); arms vs
   reads separated by grouping comments only, lanes stay hidden per ADR-0008.
   Documented, no deletion.
4. `effectHolder`/`soundHolder` inlined at their single use sites and deleted;
   `AbilityApplier` shrinks 5 methods to 3. Rotate stays bare announce (no cast
   sound by design), noted at the receiver. Shipped.
5. `PassiveEffect.planForBucket` routes Vigor through the Plan seam;
   `StanceApplier.onClassChanged` owns the Legionary marker. Shipped.
6. `ClassSelectScreen.cardModel` single-sources the hover-plus-resolve pairing
   for both render passes. (Resolve-once caching skipped: hover is per-frame.)
   Shipped.
7. `RotateAbility.auraFor` plus `TestudoAbility.windowModifiers` (shared
   instances) feed both write and remove paths. `oathFor` untouched:
   `BindMorphTest` pins ROTATE-keying, and the ROTATE-without-oath class does
   not exist. Combined mults stay owned by `dealMult`/`takeMult`, pinned by
   the stance matrix. Shipped.

GLOSSARY.md moves with the responsibilities (melee plan, ability applier,
Rend, stance applier). Full suite green (142 tests, 0 failures).
