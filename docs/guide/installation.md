# Installation

## Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft | **1.21.1** or **26.3** (one jar per version) |
| **Fabric Loader** | `>=0.19.5` |
| **Fabric API** | Any build matching your Minecraft version |
| **Java** | The runtime your game version needs: 21 (1.21.1) or 25 (26.3) |
| **Side** | Same jar on **both client and server** (server-required) |

No Forge/Quilt support. 1.20.1 and 1.21.11 are no longer supported — see [version-policy.md](version-policy.md).

## Steps

1. Install Fabric Loader and Fabric API for your Minecraft version.
2. Build or fetch the jar for your version: `./gradlew build` produces one jar per supported version under `versions/<mc>/build/libs/infusedclasses-<mod>+<mc>.jar` (currently `infusedclasses-0.5.0+1.21.1.jar` and `infusedclasses-0.5.0+26.3.jar`). The `+<mc>` suffix must match your game exactly.
3. Drop the same jar into `mods/` on both sides:
   - Singleplayer: `.minecraft/mods/`
   - Server: the server's `mods/` folder
4. Launch. First join with no class choice opens the picker automatically.

## First run

- The picker opens once, on first join when the player has no class choice. Picking validates server-side and closes only on confirm — a rejected pick stays open for retry.
- The class choice is the only saved fact (survives relog and respawn). Stacks and cooldowns are transient: they do not survive relog or respawn.
- After updates you may be told to reselect: `/class select <player>`.

## Updating

Replace the old jar with the new one, keeping the same `+<mc>` suffix as your game. If your class choice doesn't carry over, reselect: `/class select <player>`.

## Troubleshooting install problems

See [faq.md](faq.md). The usual causes are a missing/old Fabric API, the wrong Minecraft version, or the jar installed on only one side.
