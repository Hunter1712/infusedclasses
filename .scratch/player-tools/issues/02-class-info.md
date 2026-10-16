# 02: /class info + open /class root

Spec: `.scratch/player-tools/spec.md`

**What to build:** Every player can ask about themselves: bare `/class` and `/class info` speak for the invoking player — class display name, one line per lane with its remaining time in one decimal (`Rush: 18.4s`, `Rend: ready`), and the Legionary's stance. No class → one plain line. Opening the root is the risky half: the old gamemaster gate moves onto `set` and `select` (which keep it, `select` still testing-only), and the info lines are a pure decision so they cannot drift from the lane reads.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] Per-class line goldens at ready/cooling boundaries with `18.4s`-style pins; stance line present for Legionary only; no-class line; unknown/missing class safe.
- [ ] Manual: `/class` and `/class info` for each class and for a classless player; console gets vanilla's player-required failure.
- [ ] A non-op's tree shows `class` and `info` only; `/class set` and `/class select` unchanged and still refused for a non-op.
- [x] `gradle test` green.

## Comments

Implemented on `wt-player-tools`.

- Landed: pure `ClassInfo.lines(def, runtime, now)` — `Class: <displayName>` gray, one line per carried lane (`Rush: 18.4s` / `Rend: ready`), Legionary adds `Stance: <name>`, null definition yields the single gray `No class chosen`. Lane rows are gated by the definition's `actives`, so a class never shows another class's lane.
- `ClassCommands`: the root drops its `.requires` and executes the info path; `info` is its own open subcommand; `set` and `select` each carry the gamemaster gate on their own literal; `select` behaviour and copy unchanged. Info is player-only via `getPlayerOrException()` (console gets vanilla's player-required failure) and sends each line through `AbilityApplier.announce`.
- `ChatLine.cooldown` switched to `%.1fs` (shared with the status-hud lane) and the `RushAbilityTest` pin updated to `Rush: 15.0s`.
- `RotateAbility.Stance` gained `displayName()`; the fire announcement and the info line read one copy, so "Gladius"/"Scutum" cannot drift.
- `ClassInfoTest` (12 tests): per-class ready/cooling goldens, expiry boundary, no-class line, stance for Legionary only, and no key/heal copy for a class that lacks it.
- Manual in-game pass pending: per-class and classless `/class`/`/class info`, the console failure, and the non-op command tree; no `gradle deployClient` in this lane.
- Full `gradle test`: 201 tests, 0 failures.
