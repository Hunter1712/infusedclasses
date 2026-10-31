# 06: Remove 1.21.11 from metadata, docs and tooling

**What to build:** Update project metadata, documentation and developer tooling to support 1.20.1, 1.21.1 and 26.3 while removing all 1.21.11 references. The end result is accurate support statements and no stale removed-version defaults.

**Blocked by:** 01 (Multi-module build scaffolding)

**Status:** done

- [x] `fabric.mod.json` templates declare only their target version range
- [x] README, project description, compatibility and version policy list the supported versions
- [x] Installation and FAQ reference supported versions only
- [x] AGENTS and tooling defaults no longer point at the removed version
- [x] No 1.21.11 references remain in shipped metadata or docs
