# 03: Port to Minecraft 1.20.1

**What to build:** Make the 1.20.1 module compile, test and behave like the other versions by adapting version-coupled code (identifier type, save data format, mixin targets and signatures, damage and client APIs) without changing gameplay decisions. The end result is a green 1.20.1 build and passing tests.

**Blocked by:** 01 (Multi-module build scaffolding), 02 (Extract version-agnostic decisions into common module)

**Status:** done

- [x] 1.20.1 module compiles main and test sources
- [x] Unit tests pass in the 1.20.1 module
- [x] Mixins target valid 1.20.1 members with identical policy
- [x] Save data, networking and damage hooks work on 1.20.1
- [x] Client picker and status HUD compile and function
- [x] No gameplay rules differ from other versions
