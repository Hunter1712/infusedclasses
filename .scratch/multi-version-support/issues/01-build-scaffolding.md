# 01: Multi-module build scaffolding

**What to build:** A multi-module Gradle build with a shared `common` module and one Fabric module per target version (1.20.1, 1.21.1, 26.3), where the existing source tree is copied into each version module so every module compiles and tests run before any shared extraction. The end result is three independently buildable modules and a green build for all of them.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Root `settings.gradle` declares `common` and the three version modules
- [x] `common` is a plain Java library module compiling to Java 17 bytecode
- [x] Each version module applies Fabric Loom and declares its own Minecraft, loader and Fabric API versions
- [x] Each version module contains a copy of the current source and resources
- [x] `./gradlew build` succeeds for `common` and all three version modules
- [x] Existing unit tests run in at least one version module
