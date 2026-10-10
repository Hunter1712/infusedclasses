# Architecture review round 9: seven deepenings, lazy cuts

Status: done

## Problem statement

The review (report in OS temp dir, 2026-10-10) found seven shallow modules and
leaked seams: the stance adapter with 0 public verbs and 2 call sites, the
Impetus/Testudo data holders with no trigger/fire, the dual stance-settle verbs
differing only by source supplier, the Rend rename plus curve forward, the card
part-verbs composed only by cardFor, the renderer mixing pure rects with live
pixels, and the melee plan checking two ability ids for one class with a double
Impetus peek.

Prior art respected: oath dispatch stays in the entry (ADR-0009), hit ordering
stays entry-applied (ADR-0008.4), lane/window forwards stay, card model stays
the screen seam (ADR-0008.5, reopened only to demote the parts, not the model).

## Solution

Shortest diff per candidate, deletion over addition:

1. `StanceApplier` folds into `PassiveApplier` as private reconcile
   (`applyStance`/`removeStanceModifiers`/`allModifiers`); the switch marker
   inlines into `onClassChanged`. File deleted. Reopens ADR-0007's adapter
   split: one adapter was a hypothetical seam. Shipped.
2. `ImpetusAbility`/`TestudoAbility` fold into `RotateAbility`
   (`IMPETUS_NAME`/`SWITCH_DEAL_MS`/`SWITCH_DEAL_MULT`/`impetusDesc`,
   `TESTUDO_NAME`/`SWITCH_TAKE_MS`/`SWITCH_TAKE_MULT`/`testudoDesc`, outputs
   byte-identical); `IMPETUS_ABILITY`/`TESTUDO_ABILITY` ids deleted, Legionary
   carries `[ROTATE, AEQUILIBRIUM]`. Files deleted. Shipped.
3. `settleMult`/`settleMobTake` collapse into
   `settleTake(target, base, mult, Supplier<DamageSource>)`; entry passes a
   source lambda, resolution stays in the adapter. `settleMath` kept: it is the
   only game-free test seam. Shipped.
4. `RendAbility.bonusFor`/`spentPassive` deleted; callers use `BonusCurve`
   plus `empowerFor` and `def.passiveForTrigger` directly (existing null-guard
   shape). Shipped.
5. `accentFor`/`buttonFor`/`cardTextFor`/`cardStyleFor` demoted to private;
   `cardFor` + `cardLinesFor` stay the interface; screen reads the button
   through `cardFor`; pinning tests repoint at `cardFor`. Shipped.
6. Pure rects/anchors/paging/scroll move to new `CardLayout` module;
   `ClassCardRenderer` keeps only draw (`drawFrame`/`drawBody`, `wrapRows`,
   `dimmed`, `descStartFor`). Paging lives in the layout, not the screen, so it
   stays game-free testable. `wrapRows` split uses `indexOf(desc)` instead of
   length subtraction. `dimmed`/`descStartFor`/`wrapRows` stay package-visible:
   they are the only checks on tone-split math. Shipped.
7. `impetusPendingFor` gates on `ROTATE_ABILITY`; `planHit` peeks once and
   shares the answer with the deal mult through `dealWithImpetus`. Shipped
   with 2.

GLOSSARY.md synced: stance applier, Impetus, Testudo, Rend, card
accent/button/text/style anchors; new card layout term. Full suite green.

## Review follow-ups

- Code review caught a missing paper trail: recorded as
  `docs/adr/0011-fold-stance-and-flip-holders.md` (reopens ADR-0007.3 for the
  stance case) plus the `0011` line in GLOSSARY decisions.
- `wrapRows` split falls back to `full.length()` when the description is not a
  substring, preserving name tone on that edge.
- Deliberately not taken: `settleMath` inline (only game-free test seam);
  `dimmed`/`descStartFor`/`wrapRows` to private (only tone-split checks);
  `buttonFor()` inline (named internal seam); null-safe `passiveForTrigger`
  (widens the definition contract for three guarded call sites).
- `settleTake` NPE-on-throwing-supplier noted and dismissed: both entry lambdas
  capture `instanceof`-narrowed non-nulls, same as the old verbs' dead guards.
