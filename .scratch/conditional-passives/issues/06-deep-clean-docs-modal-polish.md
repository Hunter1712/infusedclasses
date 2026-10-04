# 06: Deep clean docs + modal polish

**What to build:** `AGENTS.md` and source javadoc no longer reference the deleted flat-stat system; leftover old passive copy audited; branch mergeable and ready for integration.

**Blocked by:** 01, 02, 03, 04, 05.

**Status:** done

- [x] `AGENTS.md` no longer mentions removed passive APIs (was already clean).
- [x] Source javadoc scrubbed of `Passive`, `ClassApplier`, old modifier-seam comments.
- [x] Any remaining stale passive copy (`Passives:`, `Ability:` plural issues) found and fixed — `ClassPresentation.subtitle()` returns "Your choice shapes your ability", picker uses it.
- [x] Project builds green and is integration-ready.

## Answer

No stale flat-stat references remain in source. All javadoc and comments now refer to `ConditionalPassive` or the new trigger/stack/decay system. Subtitle text updated in `ClassPresentation.subtitle()` and wired in `ClassSelectScreen`. Build and test suite green.