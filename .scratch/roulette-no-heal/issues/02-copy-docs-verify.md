# 02: Copy, docs sweep, spin verify

Spec: `.scratch/roulette-no-heal/spec.md`

**What to build:** Everything the player reads about the wheel is heal-free at the new odds: the desc template clean, the glossary entry rewritten for six bands, live guide copy swept. Then a handful of real spins on the dev client prove nothing touches HP and the announcements carry the new percents.

**Blocked by:** 01 — the copy regenerates there.

**Status:** ready-for-agent

- [ ] Desc reads the six-band text with no heal fragment.
- [ ] Glossary Roulette entry rewritten; `grep -i "vigor heal|heal 3|heal 4|seven bands" docs/ GLOSSARY.md` clean (old `.scratch` specs and ADRs stay as history).
- [ ] Manual spin pass: several spins, HP untouched, percents correct, Ward/Haste/Jackpot/Edge/Bust as before — noted in Comments.
- [ ] `gradle test` green.

## Comments

None yet.
