# ADR-0029: The ability row owns its slot and its fire

Status: accepted (supersedes ADR-0009 point 1; revises ADR-0013 point 2's fire shape and ADR-0026's target names)

## Context

The review's first candidate found the six abilities spelled across four id-keyed tables:
`ClassRegistry`'s ability-id constants, the two `BindMorph` target enums, `SlotPresses`' two
switches, and the `AbilityRoster` rows. Adding one ability meant editing a table per layer, and
the fired target and the drawn row were derived twice. ADR-0009 point 1 had kept the switch
explicit for a real reason — dispatch that needs the player (announce, cast sound, spend) is the
seam ADR-0001 draws — but each switch arm was one line: fire the target's ability, announce its
outcome, play the one fanfare.

## Decision

1. **The roster row carries the slot and the fire.** `AbilityRoster.Row` gains `Slot slot`
   (SIGNATURE/ULTIMATE), `boolean fans` and `Fire fire` — the ability's own gate-and-arm call,
   with one ignored roll argument so the wheel and the rest share one shape. The fanfare is a
   per-ability fact, so the row owns it and `SlotPresses` needs no arm.
2. **One walk, no targets.** `BindMorph.SignatureTarget`/`UltimateTarget` and
   `signatureFor`/`ultimateFor` die. `slotFor(def, slot)` walks the class's actives to the first
   roster row whose slot matches and returns the printed fallback (Offload, Onslaught) when none
   does; `shape(def)` draws the rows only for abilities the class carries.
3. **`SlotPresses` is one press.** Both receivers hand it a slot; it reads the class, bails
   classlessly (ADR-0005), fires `row.fire(...)`, and routes the outcome through one
   `announceAndCast` with `result.fired() && row.fans()`.
4. **Every fire returns `AbilityOutcome`.** Rotate's and Roulette's `fire` keep their `Result`
   records inside `trigger` (the landing is the runtime state's read, not an answer shape) and
   return the shared envelope, so the row's `Fire` has one return type.

## Consequences

- ADR-0009 point 1 is superseded: the switch leaves `SlotPresses` because firing is pure again —
  `fire` takes the class, the runtime, `now` and a roll, never the player — and the player stays
  in `SlotPresses`, which announces and plays the row's own fanfare flag.
- Every press draws the one roll the row's `Fire` takes and five rows ignore it, so a press
  advances the player's RNG stream by one draw. No rule reads stream position, and the wheel's
  band odds are unchanged; the draw keeps all six fires one shape.
- ADR-0026's outcome stands (one walk, no lending, printed fallbacks, drawn row equals fired
  row); its target names are replaced by row slots.
- ADR-0013 point 2's envelope is unchanged; its "abilities with a payload wrap" sentence no
  longer describes `fire`.
- A new ability adds one row: slot, word, riders, fanfare and fire in the one table. The keys,
  the HUD and `/class info` all walk the same rows.
