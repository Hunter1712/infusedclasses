# ADR-0016: The ultimate's own bind — Signature R, Ultimate V

Status: accepted

## Context

The two binds are inherited from the Juggernaut's kit and named for it:
`key.infusedclasses.rush` (`R`, labeled "Rush") and `key.infusedclasses.rend`
(`G`, labeled "Rend") — while per class they mean Rush/Rotate/Roulette and
Rend/Aequilibrium/All In. Since ADR-0010 gave the Legionary `G` as one clean
ultimate, the ultimate is its own concept (the bind morph's own line: "one `G`,
one ultimate per class"), and the status HUD (ADR-0014) names the two slots
SIG/ULT — but no key letter matches that vocabulary: Controls labels abilities
instead of slots, and `G` still implies a Rend heritage the ultimate no longer
has. The ask: the ultimate gets its own physical default key, the slots read
Signature and Ultimate, and the UI letters are slot names only.

## Decision

1. **Two binds by slot**: `key.infusedclasses.signature` (label "Signature",
   default `R`) and `key.infusedclasses.ultimate` (label "Ultimate", default
   `V`). `V` is unbound in vanilla; `U` remains a letter, never a key.
2. **The morph keeps its matrix, changes its vocabulary**: `BindMorph` answers
   the same targets (`SignatureTarget`: RUSH/ROTATE/ROULETTE;
   `UltimateTarget`: REND/AEQUILIBRIUM/ALL_IN) through renamed methods
   (`signatureFor`/`ultimateFor`); behaviour is byte-for-byte identical.
3. **Copy keeps physical-key notation**: ability descriptions print the actual
   defaults — `(R, …)`, and `(V, 60s)`/`(V, 50s)`/`(V, 75s)` for Rend,
   Aequilibrium and All In. Remap drift is an accepted wart, Controls being the
   truth for keys; the HUD's `SIG`/`ULT` badges are slot names and never read a
   KeyMapping.
4. **Old ids retire**: `key.infusedclasses.rush`/`key.infusedclasses.rend`
   leave the lang file, so every player starts from the new defaults once — an
   accepted one-time controls reset under the 0.x contract.
5. **No protocol churn**: `RushPayload`/`RendPayload` keep their names and
   channels; comments re-home them as the signature/ultimate slot's intents.

## Consequences

- This rewords ADR-0010's G-centric phrasing ("give `G` one clean ultimate")
  and the morph's "one `G`, one ultimate per class" line without reversing
  0010/0011's substance: the stance morph stays deleted, the oath lane and its
  50s cadence are untouched, and the entry's two-way dispatch is unchanged.
- Controls finally reads as slots; a remapped key can no longer make any
  player-facing naming lie, because copy names defaults on purpose and the
  strip carries names, not keys.
- A Juggernaut ultimate lands on `V` with no rebind; until then its `V` fires
  Rend — the known interim lie the HUD roster inherits.
- Cost: one-time bind reset for anyone who had customized either old key.
