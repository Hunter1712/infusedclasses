# Version policy

- **Supported: Minecraft 1.20.1, 1.21.1 and 26.3**, on Fabric only. Each jar's metadata declares a single-version range (`~<mc>`).
- **1.21.11 is no longer supported.** After its removal the mod ships no 1.21.11 artifact; reports against it close as wontfix. Move to the nearest supported version — see the compatibility table in [compatibility.md](compatibility.md).
- **One jar per game version** (`infusedclasses-<mod>+<mc>.jar`, e.g. `infusedclasses-0.5.0+1.21.1.jar`), so each artifact picks up its own version at launch with no rename and no cross-loading. Local builds use the same suffixed names.
- **The build is multi-module.** A shared `common` module holds the version-agnostic decisions and their tests; one Fabric module per supported version holds the version-coupled adapters, mixins, networking, client code and resources. Gameplay decisions are written once, so class balance and behaviour do not differ by Minecraft release — see [ADR-0033](../../adr/0033-multi-module-version-support.md).
- **Fabric API / Loader versions** per target are pinned in that version module's `gradle.properties`, matched against the installed environments at build time.
- **No Forge/Quilt support** on any version.
