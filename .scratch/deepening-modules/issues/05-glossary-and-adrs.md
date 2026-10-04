# 05: Record the domain vocabulary and the architectural decisions

**What to build:** This work names modules after concepts that do not yet exist in the project's domain docs, and settles three decisions that a future architecture review would otherwise re-propose from scratch. Create the glossary and record the decisions.

**Blocked by:** 02 — Split per-player state at its pure/live seam; 03 — Ability outcome carries what the adapter applies; 04 — Card lines carry style, card drawing moves behind one module

**Status:** done

- [x] A glossary exists and names the concepts this work introduced: the passive effect, the applied outcome, the transient stacks module, and the choice module.
- [x] An ADR records the decide-purely-apply-in-an-adapter rule, including that the passive effect, the ability outcome and the card style are three instances of one rule rather than three seams.
- [x] An ADR records that transient per-player state is owned by the server entry, as settled by ticket 02.
- [x] An ADR records the deferred class lookup seam: one adapter exists today, which is a hypothetical seam, so it is not built until a second source of classes appears.
- [x] The deferred seam ADR states the trigger condition for building it, so a future review does not re-propose it as new.
- [x] The architecture-review trigger in the project's agent instructions no longer describes a pattern that has been retired.

**Note:** candidate 5 from the architecture review (abstracting class lookup behind a seam) is deliberately not an implementation ticket. The spec places building it out of scope; it is recorded here as a decision instead.