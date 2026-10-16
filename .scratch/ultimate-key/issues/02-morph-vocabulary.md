# 02: Morph + receivers to slot vocabulary

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** No visible change — the code names catch up with the slots: the bind morph answers `signatureFor` (RUSH/ROTATE/ROULETTE) and `ultimateFor` (REND/AEQUILIBRIUM/ALL_IN), the receiver comments and switches use slot language, and the payload classes keep their names with a comment re-homing each as its slot's intent. Behaviour is byte-for-byte identical; the renamed test is the proof.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Every old assertion has a 1:1 successor in the renamed test; nothing added.
- [x] `grep -rn "rushFor\|oathFor\|RushTarget\|OathTarget" src/` clean.
- [x] `gradle test` green with zero behavioural delta.

## Comments

- 2026-10-16 (wt-ultimate-key): `BindMorph` speaks slots — `signatureFor`/`SignatureTarget` (RUSH/ROTATE/ROULETTE), `ultimateFor`/`UltimateTarget` (REND/AEQUILIBRIUM/ALL_IN), javadoc rewritten, ability-id checks and order untouched. `ServerEvents` receiver comments and switch calls renamed to slot language; no logic change. `RushPayload`/`RendPayload` keep their names and channels, comments re-homed as the signature/ultimate slot's intents. `BindMorphTest` renamed: every old assertion has a 1:1 successor in the same order (4 + 4 + 6), nothing added — the diff is renames only, so behaviour is byte-for-byte identical. Both verification greps return nothing; `gradle test`: 181 tests, green.
