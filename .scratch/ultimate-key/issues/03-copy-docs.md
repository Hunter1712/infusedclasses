# 03: Copy + docs: (V, …) and the classes page

Spec: `.scratch/ultimate-key/spec.md`

**What to build:** Every player-facing mention of the ultimate names the real default: the Rend/Aequilibrium/All In descriptions read `(V, …)`, guide and glossary letters update, the "one `G`, one ultimate" phrasing goes, and the classes page stops saying two classes — the Gambler gets its section, with card copy as the source so the wheel text arrives already heal-free. History (old specs, old ADRs) stays as written.

**Blocked by:** 01 — don't advertise `V` before it is bound. Also `.scratch/roulette-no-heal` 01 (wheel text heal-free before the classes page is written) and `.scratch/player-tools` 04 (it rewrites `commands.md` first — the key line here follows).

**Status:** done

- [x] `grep -rn "(G," src/main docs/guide GLOSSARY.md` clean; the ability-test pins updated.
- [x] `grep -rn "key\.infusedclasses\.\(rush\|rend\)" src docs GLOSSARY.md` clean (the lang file included).
- [x] `classes.md` lists three classes with accurate key letters; faq, compatibility and the `commands.md` key line updated.
- [x] `gradle test` green.

## Comments

Implemented on `wt-ultimate-key-docs`. Both verification greps clean. Ability descs and their pins: `RendAbility`/`AequilibriumAbility`/`AllInAbility` `desc()` read `(V, …)`, `RushAbility`/`RotateAbility`/`RouletteAbility` keep `(R, …)`; test pins updated in `AllInAbilityTest` (75s (V, …)) and `LegionaryTest` (oath `(V, 50s oath)`). `classes.md` reads "Three playable classes" and gains the Gambler section derived from the live card copy (`ClassRegistry` + `RouletteAbility.desc()`, `AllInAbility.desc()`, the `CritRule`-generated Crooked Dice/Streak lines): six heal-free bands, ten-second stat windows, All In lifts the crit mult and bends nothing else. `faq.md`'s question is now "R and V do different things per class?", answered in slot vocabulary (Signature/Ultimate, Gambler rows included) with the strip-names-slots / Controls-is-truth note; `compatibility.md`'s client row reads `R` Signature / `V` Ultimate; `commands.md` — only the picker-behaviour key line touched (`R` roulette/rotate/Rush, `V` oath/All In/Rend). GLOSSARY: Rend, Rotate, Aequilibrium, All In and Bind morph speak slots ("one ultimate per class" replaces the one `G` line; Rotate's bind line is the Signature bind, shared by every class), and Rush/Rend intent are re-anchored on `key.infusedclasses.signature` / `key.infusedclasses.ultimate` with an explicit note that the payload classes and channels keep their `rush`/`rend` names on purpose. Two small deltas outside the literal desc() contract, noted here: the three `{@code G}` class javadocs (Rend/Aequilibrium/All In) now say `{@code V}` so the code stops claiming a key the copy retired, and ADR-0016 names the retired ids as the short `rush`/`rend` ids (substance unchanged) so its own context doesn't trip the second grep. `gradle test` green: 241 tests, 0 failures, 0 errors, 0 skipped (28 suites). Version bump/changelog stays with 04; the manual Controls pass stays with 01's note.
