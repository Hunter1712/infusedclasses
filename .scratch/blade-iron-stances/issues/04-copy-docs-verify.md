# 04: Picker copy, glossary, ADR, and full verify

Spec: `.scratch/blade-iron-stances/spec.md`

**Blocked by:** 02, 03 (needs final constants and behavior).

**Status:** done

## Task

Docs and proof. No behavior changes unless verification finds drift:

- `ClassDefinition.ActiveAbility` descs generated from the new constants
  (Rotate lock + stance announces, Impetus bonus/taken/window, Testudo
  armor/deal/window, shared 50s lane). Card body shows stance names only;
  tooltip shows full form. No `Roots` text anywhere; no HUD rows.
- `GLOSSARY.md`: Legionary, Gladius, Scutum, Rotate, Impetus, Testudo,
  BonusCurve, stack gate. One ADR for the stance + quad decisions.
- Verify: `gradle test` green; `gradle deployClient` rebuilds and refreshes
  the live jar; in-game `/class select <player>` shows the Legionary card
  with correct copy, `R` flips stance with its announce, `G` pays per stance
  on the shared lane, spam earns no stacks and quarter bonus at half charge.

## Acceptance

- Copy matches live numbers exactly (grep constants vs card text); glossary
  and ADR landed; `gradle test` green; manual dev-client pass done.
- Spec `Status:` flipped to done when this ticket lands.

## Comments

Done: copy from constants, glossary + ADR-0006, `gradle test` green,
`gradle deployClient` green. Manual in-game pass still open: `/class select`
card, `R` flip announce, `G` per stance on shared lane, spam vs timed.
