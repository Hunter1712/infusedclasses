# Spec: The ultimate's own bind — Signature R, Ultimate V

Status: ready-for-agent

## Problem Statement

Both reuse-binds carry the Juggernaut's original names: `key.infusedclasses.rush` (`R`) and `key.infusedclasses.rend` (`G`), labeled "Rush" and "Rend" in Controls while they actually mean Rush/Rotate/Roulette and Rend/Aequilibrium/All In per class. Since 0.2.0 the ultimate is its own concept ("one G, one ultimate", ADR-0010), and the status HUD (ADR-0014) names the two slots SIG/ULT — but no key letter matches that vocabulary, Controls labels abilities instead of slots, and `G` still implies a Rend heritage the ultimate no longer has. The ask: the ultimate gets its own physical default key, the slots read Signature and Ultimate, and the UI letters are slot names only, never keys.

## Solution

- **Two binds, by slot**: `key.infusedclasses.signature`, label **Signature**, default `R` (Rush/Rotate/Roulette) and `key.infusedclasses.ultimate`, label **Ultimate**, default `V` (Rend/Aequilibrium/All In). `V` is unbound in vanilla and sits under the left index; `T` was rejected (Open Chat) and `U` remains a letter, never a key.
- **The morph keeps its matrix, changes its vocabulary**: `BindMorph` answers the same targets per bind; methods and enums take slot language (`signatureFor`/`SignatureTarget`, `ultimateFor`/`UltimateTarget`) so the names describe what they are.
- **Copy keeps physical-key notation**: ability descriptions print the real defaults — `(R, …)` unchanged and `(V, 60s)`/`(V, 50s)`/`(V, 75s)` for Rend, Aequilibrium and All In. Remap drift is an accepted wart; the guide tells players the strip names slots and Controls is the truth for keys.
- **Old ids retire**: `key.infusedclasses.rush`/`key.infusedclasses.rend` are replaced by the two new ids, so every player starts from the new defaults once — an accepted one-time controls reset, noted in the changelog.
- **No protocol churn**: the C2S payloads keep their names and channels (`RushPayload`/`RendPayload` are the signature/ultimate slot's intents now; comments say so).

## User Stories

1. As a player, I want Controls to say "Signature" and "Ultimate", so the labels mean the slots, not one class's abilities.
2. As a Juggernaut, I want the ultimate on its own key (`V`), so pressing it can never be confused with the signature press.
3. As a Legionary or Gambler, I want the same two binds to keep working across classes, so switching never makes me relearn keys.
4. As a player who remaps keys, I want the HUD to keep showing slot names, so the strip cannot lie about my controls.
5. As a player, I want copy to name real default keys (`(R, …)`, `(V, …)`), so I can follow it out of the box.
6. As a maintainer, I want one slot vocabulary in code (`SignatureTarget`/`UltimateTarget`), so bind decisions read as slots.

## Implementation Decisions

- `client/InfusedClassesClient`: keybind ids, labels and defaults per the Solution; variables renamed `signatureKey`/`ultimateKey`; the payload sends and the consume-click loop are unchanged.
- `lang/en_us.json`: two new entries (`Signature`, `Ultimate`); the old two deleted (one-time bind reset).
- `BindMorph`: `rushFor` → `signatureFor`, `RushTarget` → `SignatureTarget`, `oathFor` → `ultimateFor`, `OathTarget` → `UltimateTarget`; javadoc rewritten in slot language; behaviour byte-for-byte identical (same ability-id checks, same order).
- `ServerEvents` receivers: comments and switch names land on slot language; no logic change. The payload class names stay (`RushPayload`/`RendPayload`) with comments re-homing them as the signature/ultimate slot's intents — renaming wire channels is churn with no player-facing value.
- `RendAbility.desc()`, `AequilibriumAbility.desc()` and `AllInAbility.desc()`: `(G, …)` → `(V, …)`. `RushAbility`, `RotateAbility` and `RouletteAbility` keep `(R, …)`.
- `BindMorphTest`: test names and enum references renamed; every old assertion gets a 1:1 successor, nothing new added (the matrix is already pinned). `AllInAbilityTest` and `LegionaryTest` desc pins update; a `(G,` grep over `src/` comes back empty.
- Docs pass: `docs/guide/classes.md` (the `G` letters move to `V`; add the missing Gambler section — the page still says "Two playable classes" while three exist, and the card copy is the source for its text), `docs/guide/faq.md` ("R and G do different things" question), `docs/guide/compatibility.md` (the client row's keybind list), `docs/guide/commands.md` (the picker-behaviour key line only — its command sections are `.scratch/player-tools` issue 04's), and `GLOSSARY.md`: Rend, Rotate, Aequilibrium, All In, Bind morph, Rush intent, Rend intent — plus deleting the "one `G`, one ultimate" phrasing and re-anchoring the intent entries on the new key ids.
- ADR-0016 records the decision and the re-wording of ADR-0010/0011's G-centric statements (substance kept: no stance morph, oath lane untouched).
- Release metadata (0.3.0 bump, CHANGELOG entry) is issue 04 here, landing after the other three specs' code.

## Testing Decisions

- `BindMorphTest` renamed and green with zero behavioural delta.
- The updated desc pins compile and pass; the `(G,` grep over `src/`-live code and guide docs returns nothing (old `.scratch` specs and old ADRs are history and stay as written).
- Not unit-tested (manual dev pass): the two KeyMappings' labels and defaults in Controls, and the one-time reset — launch the dev client behind `gradle deployClient`, open Controls, see Signature `R` / Ultimate `V`, press both.
- `gradle test` green.

## Out of Scope

- Renaming the C2S payload classes or channels (`RushPayload`/`RendPayload` stay; the wire is not the player surface).
- A real Juggernaut ultimate (its `V` fires Rend until one exists — the known interim lie; the HUD roster swaps when the ability lands).
- Remap-aware copy or HUD (slot names are the truth by design; copy names defaults on purpose).
- Alternative default letters (`U`/`T` were considered; `V` chosen).

## Further Notes

- The user's asks, three rounds apart: signature stays `R`; the ultimate's physical default becomes `V`; the UI letters are slot names and live on the HUD chips only — copy never spells the slot names as keys.
- The old key ids were also the glossary's intent anchors. Entries update, payload names stay, so protocol vocabulary and player vocabulary diverge on purpose; the glossary edits say so.
- One-time bind reset: anyone who had rebound either old key loses their custom bind once. The 0.x alpha contract covers resets; the changelog notes it.
