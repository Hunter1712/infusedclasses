# ADR-0026: The ultimate is not lent

Status: accepted

## Context

The bind matrix keyed the ultimate on the signature row: ADR-0009 point 3 single-sourced both
morph checks on the Legionary's `ROTATE` marker through `hasAbility`, and ADR-0023 point 2
kept the consequence — "the ultimate bind keys on the `ROTATE` row while the drawn row is
Aequilibrium's (ADR-0009)". The lending was inert where it mattered and wrong where it was
reachable:

- Rend's `trigger` rejects for a class not carrying `REND_ABILITY`, so a class with Rotate and
  no Rend fired nothing at all — the "lent" ability was never armed.
- A class carrying Aequilibrium but no Rotate fired Rend instead of its own ultimate, so the
  drawn row and the fired ability could disagree by construction.

The check also restated the ability ids outside the classes that own them: the HUD's signature
and ultimate chains had their own hardcoded candidate lists, three spellings of one matrix.

## Decision

1. **Each target names the ability it fires**: `SignatureTarget.ability()` and
   `UltimateTarget.ability()` (RUSH/ROTATE/ROULETTE and REND/AEQUILIBRIUM/ALL_IN).
2. **One walk of the class's own actives**: `signatureFor` / `ultimateFor` return the target of
   the first active naming a target ability; the printed fallback stays (Rush, Rend) for a
   classless player or a definition carrying neither.
3. **`shape(def)` returns the whole class shape**: both targets and the roster rows that draw
   them (`signatureRow`, `ultimateRow`), the rows null when the class does not carry the
   ability. The HUD's candidate lists and its own row picks die; the drawn row is the fired
   target's own row, so the key and the stack cannot disagree.
4. **The lent ultimate leaves**: no class fires an ability it does not carry. The entry's
   dispatch is untouched — the receivers still switch on the target and keep their arms
   (ADR-0009's main point).

## Consequences

- For every registered class the resolved target is unchanged (each carries its ultimate
  ability); the difference is reachable only for definitions the registry does not carry,
  where the old behaviour was either inert (Rotate without Rend) or wrong (Aequilibrium
  without Rotate fired Rend).
- ADR-0009 point 3 and ADR-0023 point 2's ultimate-bind sentence are revised; 0009's
  entry-stays-explicit decision is not.
- A new class adds actives and nothing else: no HUD branch, no morph branch, no second list.
- Drift pin: `BindMorphTest` walks hand-built definitions (Rotate alone falls back to Rend
  with no ultimate row; Aequilibrium without Rotate now fires Aequilibrium), and
  `AbilityRosterTest` pins that every registered active draws a row.
