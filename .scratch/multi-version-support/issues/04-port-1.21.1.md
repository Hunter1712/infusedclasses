# 04: Port to Minecraft 1.21.1

**What to build:** Make the 1.21.1 module compile, test and behave like the other versions by adapting version-coupled code to 1.21.1 mappings and APIs. The end result is a green 1.21.1 build and passing tests.

**Blocked by:** 01 (Multi-module build scaffolding), 02 (Extract version-agnostic decisions into common module)

**Status:** done

- [x] 1.21.1 module compiles main and test sources
- [x] Unit tests pass in the 1.21.1 module
- [x] Mixins target valid 1.21.1 members with identical policy
- [x] Networking, damage hooks and client UI work on 1.21.1
- [x] No gameplay rules differ from other versions
