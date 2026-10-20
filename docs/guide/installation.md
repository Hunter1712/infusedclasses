# Installation

## Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft | **1.21.11** only |
| **Fabric Loader** | `>=0.19.5` |
| **Fabric API** | Any build matching 1.21.11 |
| **Java** | 21 |
| **Side** | Same jar on **both client and server** (server-required) |

No Forge/Quilt support. No 1.20.x backports in 0.x. A 26.x port comes later as a separate branch — see [version-policy.md](version-policy.md).

## Steps

1. Install Fabric Loader for 1.21.11 plus Fabric API.
2. Build or fetch the jar: `./gradlew build` produces `build/libs/infusedclasses-<version>.jar` (currently `infusedclasses-0.5.0.jar`). At listing time jars carry the game version (`infusedclasses-<mod>+1.21.11.jar`) — the `+1.21.11` suffix must match your game exactly.
3. Drop the same jar into `mods/` on both sides:
   - Singleplayer: `.minecraft/mods/`
   - Server: the server's `mods/` folder
4. Launch. First join with no class choice opens the picker automatically.

## First run

- The picker opens once, on first join when the player has no class choice. Picking validates server-side and closes only on confirm — a rejected pick stays open for retry.
- The class choice is the only saved fact (survives relog and respawn). Stacks and cooldowns are transient: they do not survive relog or respawn.
- After updates you may be told to reselect: `/class select <player>`.

## Updating

Replace the old jar with the new one. If your class choice doesn't carry over, reselect: `/class select <player>`.

## Troubleshooting install problems

See [faq.md](faq.md). The usual causes are a missing/old Fabric API, the wrong Minecraft version, or the jar installed on only one side.
