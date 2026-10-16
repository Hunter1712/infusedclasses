# 02: Morph + receivers to slot vocabulary

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** No visible change — the code names catch up with the slots: the bind morph answers `signatureFor` (RUSH/ROTATE/ROULETTE) and `ultimateFor` (REND/AEQUILIBRIUM/ALL_IN), the receiver comments and switches use slot language, and the payload classes keep their names with a comment re-homing each as its slot's intent. Behaviour is byte-for-byte identical; the renamed test is the proof.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Every old assertion has a 1:1 successor in the renamed test; nothing added.
- [ ] `grep -rn "rushFor\|oathFor\|RushTarget\|OathTarget" src/` clean.
- [ ] `gradle test` green with zero behavioural delta.

## Comments

None yet.
