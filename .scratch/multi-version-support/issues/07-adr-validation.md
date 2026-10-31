# 07: ADR and per-version validation

**What to build:** Record the multi-module version strategy as an ADR and complete in-game validation on one instance per supported version. The end result is a documented decision and verified mixin/client behavior across versions.

**Blocked by:** 03 (Port to Minecraft 1.20.1), 04 (Port to Minecraft 1.21.1), 05 (Port to Minecraft 26.3)

**Status:** done

- [x] ADR documents the multi-module version strategy
- [x] ADR is indexed in the ADR index
- [x] Each supported version builds and its tests pass
- [x] In-game verification passes on each version: picker, Momentum, crit cue, shield-eaten hits, settle health drop, status HUD
- [x] Validation checklist in the spec is completed
