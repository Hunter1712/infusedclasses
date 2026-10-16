# 03: Copy + docs: (V, …) and the classes page

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** Every player-facing mention of the ultimate names the real default: the Rend/Aequilibrium/All In descriptions read `(V, …)`, guide and glossary letters update, the "one `G`, one ultimate" phrasing goes, and the classes page stops saying two classes — the Gambler gets its section, with card copy as the source so the wheel text arrives already heal-free. History (old specs, old ADRs) stays as written.

**Blocked by:** 01 — don't advertise `V` before it is bound. Also `.scratch/roulette-no-heal` 01 (wheel text heal-free before the classes page is written) and `.scratch/player-tools` 04 (it rewrites `commands.md` first — the key line here follows).

**Status:** ready-for-agent

- [ ] `grep -rn "(G," src/main docs/guide GLOSSARY.md` clean; the ability-test pins updated.
- [ ] `grep -rn "key\.infusedclasses\.\(rush\|rend\)" src docs GLOSSARY.md` clean (the lang file included).
- [ ] `classes.md` lists three classes with accurate key letters; faq, compatibility and the `commands.md` key line updated.
- [ ] `gradle test` green.

## Comments

None yet.
