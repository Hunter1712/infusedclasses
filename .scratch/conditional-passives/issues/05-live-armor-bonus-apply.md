# 05: Live armor bonus apply

**What to build:** Current stack count yields a real `minecraft:armor` `ADD_VALUE` modifier on the player, replaced each stack change.

**Blocked by:** 03.

**Status:** ready-for-agent

- [ ] Stack count maps to `stacks * 1.0` armor value.
- [ ] Old modifier is removed and new one added on every stack change.
- [ ] Stacks of 0 remove the modifier entirely.
