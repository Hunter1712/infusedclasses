# 03 — One class shape

Status: done

## Context

Two mechanisms answer "which rows are the class's signature and ultimate lines":
`BindMorph.signatureFor`/`ultimateFor` walk the class's actives through the roster's bind
columns (the authority for what the keys fire), and `StatusHud.signatureLane`/`ultimateLane`
walk a hardcoded candidate list (`RUSH, ROTATE, ROULETTE` / `REND, AEQUILIBRIUM, ALL_IN`) in
roster order. The two agree only because today's three classes happen to list their actives in
roster order; they disagree for a class carrying two signature-shaped actives, and the HUD's
list cannot learn about a new ability without an edit (ADR-0023 §2 kept the pick in the rows
decision for exactly this reason). Round-13 candidate 3.

## Deliverable

- `SignatureTarget`/`UltimateTarget` carry the ability each target fires (`ability()`);
  `BindMorph` resolves an ability id to its target through one private lookup per enum
  (`signatureTargetOf`/`ultimateTargetOf`), not a scan repeated per caller.
- `BindMorph.signatureFor`/`ultimateFor` walk the class's actives through those ids — one
  mechanism, order-independent of roster order.
- `BindMorph.shape(def)` returns `Shape(signatureTarget, signatureRow, ultimateTarget,
  ultimateRow)`: the targets the slots fire and the rows that draw them (the target's own
  ability row, null when the class does not carry it). `StatusHud` reads the shape and the
  hardcoded candidate lists die.
- The ultimate no longer keys on the ROTATE row: the mortgage is recordable as inert (a
  Rotate-only class had V fire Aequilibrium, which rejects for it) and the drawn ultimate row
  and the fired ultimate are now the same fact. ADR-0026 records it.

## Done when

- `StatusHudTest`'s per-class row picks are unchanged (Juggernaut, Legionary, Gambler), and
  `BindMorphTest`'s morph pins move to the walk's own answer.
- `shape` returns null rows for a class carrying none of the slot abilities, and a class
  carrying two signature-shaped actives draws the first *active's* row, the same one the bind
  fires.
