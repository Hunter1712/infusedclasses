# 05: Live armor bonus apply

**What to build:** Current stack count yields a real `minecraft:armor` `ADD_VALUE` modifier on the player, replaced each stack change.

**Blocked by:** 03.

**Status:** done

- [x] Stack count maps to `stacks * 1.0` armor value.
- [x] Old modifier is removed and new one added on every stack change.
- [x] Stacks of 0 remove the modifier entirely.

## Answer

Implemented in `InfusedClasses.applyConditionalArmorModifier()` called from server tick. Finds `Bulwark` passive, reads stacks from `PlayerClasses`, applies/removes `minecraft:armor` modifier with id `infusedclasses:bulwark_armor`. Only writes when value actually changes.