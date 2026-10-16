# 03: /class pick (cheats-gated self-picker)

Spec: `.scratch/player-tools/spec.md`

**What to build:** Anyone with cheats can reopen their own picker: `/class pick` sends the same picker signal `select` sends, landing on the current class's page, with no chat output. Without cheats the command is not in the tree at all — vanilla syncs only nodes whose requirements pass, so there is nothing to reject and no rejection copy anywhere. The gate is the per-player permission ≥ gamemaster check `set` and `select` already carry.

**Blocked by:** 02 — the same command file, and the root opens there.

**Status:** done

- [ ] As a player without cheats: `/class pick` is not offered and fails as an unknown command; `/class info` works.
- [ ] As an op (or singleplayer-with-cheats): `/class pick` opens the picker on the current page for a classed and a classless player; `/class select <player>` still works unchanged.
- [x] `gradle test` green.

## Comments

Implemented on `wt-player-tools`.

- Landed: `pick` literal under the open root with its own gamemaster gate, self-only (no argument, `getPlayerOrException()`), sends the same `OpenClassScreenPayload` `select` sends (current class id, empty when none) through the shared `openPicker` helper, and prints nothing — the picker is the feedback.
- The gate is the same predicate constant `set`/`select` use, so "has cheats" has one definition in the command file.
- Deviations: none. Nothing else about the picker flow changed (rejected picks stay open, close only on confirm).
- Manual in-game pass pending: the hidden/visible tree per cheats, a classed and classless pick landing on the current page, and `select` unchanged; no `gradle deployClient` in this lane.
- Full `gradle test`: 201 tests, 0 failures.
