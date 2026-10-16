# 01: Snapshot decision + the reads it needs

Spec: `.scratch/status-hud/spec.md`

**What to build:** The mod can describe a player's whole strip as one pure, goldened value — a new snapshot record (`PlayerStatusPayload`) holding class, every lane's remaining time, every live window and its load flags, Momentum stacks with grace, stance, the flip, Streak and the Roulette outcome. Built only from the transient runtime and a `now`, with an equality guard (`movedSince`) that treats a quiet second as no change — the entire reason the strip costs no packets between moves. The runtime gains only the reads this needs: remaining-millis verbs for the six windows and a passive's grace remaining, each a one-line forward in the house shape (never the private `Window`).

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Record holds every field the spec's roster needs, as numbers, booleans, strings and pure enums only.
- [x] Builder goldens per class at a fixed `now`: fresh runtime (ready lanes, no windows, no stacks), after a fire (lane armed, window live), after a settle (stacks/Streak moved), spent vs unspent one-shots.
- [x] `movedSince` false for a rebuild one second later with nothing changed; true for each discrete change (lane armed, window armed, stack bump, Streak move, stance flip, outcome set, class switch).
- [x] Expiry boundary pinned (1 ms left vs 0).
- [x] Runtime tests extended for each new read (0 when dead, exact values).
- [x] `gradle test` green; the decision touches no player, level or clock beyond the passed `now`.

## Comments

Landed as `PlayerStatusPayload` with a hand-rolled `StreamCodec<FriendlyByteBuf, ...>` (22 fields outgrow
the composite overloads; enums travel by name, empty name = null). `classId` is a `String` the way
`OpenClassScreenPayload` carries it: empty is the classless state, a registry-unknown id travels verbatim.
`flipTarget` is derived in the builder as the stance while the flip window runs (null otherwise): a flip
lands in the stance it targets, so no extra runtime read was needed beyond the six window remainders and
`graceRemaining`. `movedSince` compares lane/window *armed* booleans (raw remainders decay every rebuild)
plus every discrete field; crossing an armed boundary counts as a move, which is what makes a re-arm after
an expiry visible again while a quiet second stays silent. Verified: `gradle test`, 28 focused tests green
(201 across the suite at this commit).

