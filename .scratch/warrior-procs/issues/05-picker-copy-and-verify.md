# 05: Picker copy, docs, and in-game verification

**What to build:** Player-visible copy for the two procs plus the manual pass no test can cover. `ClassRegistry` Warrior entry (and `ClassPresentation` card/tooltip lines) names Second Wind alongside War Cry with honest numbers; `GLOSSARY.md` and any touched ADR record the proc vocabulary. Then a dev-client run via `gradle deployClient` (never `compileJava` alone for client-visible work) proving the fantasy end to end.

**Blocked by:** 03, 04

**Status:** ready-for-human

- [x] Picker card/tooltip for Warrior describes War Cry extension (25% per charged hit, +2s, no cap) and Second Wind (`G`, 60s, half heart per Bulwark stack, consumes stacks); copy generated from or checked against the 01/02 constants so numbers cannot drift
- [x] `GLOSSARY.md` records `proc`, `Second Wind`, `War Cry extension` if those terms stick; docs touched only where behavior changed
- [ ] In-game check via `gradle deployClient` plus `/class select <player>`: hold Strength II with charged swings, confirm per-victim rolls and indefinite upkeep while aggressive, and that death/milk/disengaging ends it
- [ ] In-game check: Second Wind at 0 stacks prints `No Bulwark` and burns no cooldown; at 5/10 stacks heals 5/10 HP, drops armor to zero, and the Bulwark row clears; `R` and `G` cooldowns advance independently
- [ ] In-game check: War Cry row shows seconds-left plus drain bar, updates on fire and every proc, never covers Bulwark, and no vanilla Strength box shows alongside (iconless effect plus inventory filter)
- [x] No balance changes beyond 25% / +2s / 1.0 HP-per-stack / 30s / 60s; no deterministic/pity procs, no soft caps

## Comments

Copy: Warrior actives listed as `War Cry` and `Second Wind` (one card line and one
tooltip entry each), each description built from the 01/02/Cooldown
constants (tooltip test asserts every number against the constants); `en_us.json` gains the `G`
binding; glossary gains proc / Second Wind / War Cry extension / war cry signal, row, and store
terms plus updated Cooldown/Class/intent entries, no new ADR (decide-then-apply already covers
it). `gradle deployClient` succeeded — jar live in the 1.21.11 instance mods. The three in-game
checks above need a human in the client: `/class select <player>` to reopen the picker.

Triage: stays ready-for-human, narrowed — the two War Cry checks are moot (deleted, spec
superseded). The Second Wind check (0-stack fizzle, 5/10-stack heal, independent cooldowns)
is still open, plus the Berserk feel pass (damage/speed rising as HP falls, picker copy).
