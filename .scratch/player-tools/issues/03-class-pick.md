# 03: /class pick (cheats-gated self-picker)

Spec: `.scratch/player-tools/spec.md`

**What to build:** Anyone with cheats can reopen their own picker: `/class pick` sends the same picker signal `select` sends, landing on the current class's page, with no chat output. Without cheats the command is not in the tree at all — vanilla syncs only nodes whose requirements pass, so there is nothing to reject and no rejection copy anywhere. The gate is the per-player permission ≥ gamemaster check `set` and `select` already carry.

**Blocked by:** 02 — the same command file, and the root opens there.

**Status:** ready-for-agent

- [ ] As a player without cheats: `/class pick` is not offered and fails as an unknown command; `/class info` works.
- [ ] As an op (or singleplayer-with-cheats): `/class pick` opens the picker on the current page for a classed and a classless player; `/class select <player>` still works unchanged.
- [ ] `gradle test` green.

## Comments

None yet.
