# 02: Copy, docs sweep, spin verify

Spec: `.scratch/roulette-no-heal/spec.md`

**What to build:** Everything the player reads about the wheel is heal-free at the new odds: the desc template clean, the glossary entry rewritten for six bands, live guide copy swept. Then a handful of real spins on the dev client prove nothing touches HP and the announcements carry the new percents.

**Blocked by:** 01 — the copy regenerates there.

**Status:** done

- [x] Desc reads the six-band text with no heal fragment.
- [x] Glossary Roulette entry rewritten; `grep -i "vigor heal|heal 3|heal 4|seven bands" docs/ GLOSSARY.md` clean (old `.scratch` specs and ADRs stay as history).
- [ ] Manual spin pass: several spins, HP untouched, percents correct, Ward/Haste/Jackpot/Edge/Bust as before — noted in Comments.
- [x] `gradle test` green.

## Comments

Landed: the GLOSSARY's Roulette entry is rewritten for the six bands at 25/25/25/10/10/5 with no heal sentence, and the desc regenerates `One roll: Edge 25% x1.25, Ward 25% +2 armor, Haste 25% +0.02 speed, Jackpot 10% all, Bust 10% x0.75, Zero 5%; 10s (R, 25s).` (pinned in ticket 01). `git grep -inE "vigor heal|heal 3|heal 4|seven bands" -- GLOSSARY.md docs/` matches only `docs/adr/0015` (exempt history). Sweep note: `CHANGELOG.md`'s 0.2.0 entry still reads "seven bands"; it records what 0.2.0 shipped and sits outside this ticket's `docs/ GLOSSARY.md` sweep, so it was left as history. `gradle test` green (181 tests, 23 classes).

**Manual in-game spin pass pending**: several spins, HP untouched, percents correct, Ward/Haste/Jackpot/Edge/Bust as before. Not run here — the lane was told not to `deployClient` or attempt in-game play; the orchestrator/human checks this later on the dev client.
