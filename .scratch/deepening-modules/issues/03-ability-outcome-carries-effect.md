# 03: Ability outcome carries what the adapter applies

**What to build:** The ability decision already happens purely and returns a result, but the mod entry class then hardcodes what to do with it: which effect, for how long, at what amplifier, plus sound, particles and chat. Move those facts into the result, so the entry class maps one result to one outcome instead of re-deriving it. Formatting the remaining cooldown seconds also becomes part of what the result decides rather than something the entry class formats.

From the player's side nothing changes: War Cry still gives Strength for eight seconds on a thirty-second cooldown, the cooldown message still counts down in whole seconds, and a wrong-class or on-cooldown press still does nothing.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The ability result carries the effect facts needed to apply it — duration, amplifier — instead of the entry class holding them.
- [ ] The on-cooldown message text, including rounding and locale, is produced by the decision and is assertable.
- [ ] The wrong-class and on-cooldown results still short-circuit before any effect, sound, particle or chat is applied.
- [ ] The three existing result kinds are unchanged.
- [ ] A second ability can be defined without copying the War Cry application method.
- [ ] The application of the outcome lives in one place; the entry class maps the result and delegates.
- [ ] New assertions cover the result's effect duration, amplifier and cooldown message formatting.
- [ ] Existing ability and cooldown tests pass unchanged.
- [ ] War Cry's gameplay numbers are unchanged: eight second duration, thirty second cooldown.