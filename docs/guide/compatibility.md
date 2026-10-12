# Compatibility

## Supported versions

One project page, one jar per game version. Download the jar whose `+<mc>` suffix matches your game version.

| Mod loader | Minecraft | Java | Jar |
|------------|-----------|------|-----|
| Fabric | 1.21.11 | 21+ | `infusedclasses-<mod>+1.21.11.jar` |

No Forge/Quilt support. No 1.20.x backports in 0.x. A 26.x port comes later as a separate branch — see [version-policy.md](version-policy.md).

## Dependencies

| Dependency | Required? | Notes |
|------------|-----------|-------|
| **Fabric Loader** | Yes | `0.19.5` or newer |
| **Fabric API** | Yes | Any build matching 1.21.11 |
| Anything else | No | No hard dependencies |

## Client / server

| Side | Required | Why |
|------|----------|-----|
| Server | **Yes** | Class choice validation, ability gating and arming, hit planning and settling, stance auras — all server-side and server-authoritative |
| Client | **Yes** (supported configuration) | Picker screen, keybind intents (`R` Rush/Rotate, `G` Rend/Oath), pick confirm handling |

The client only sends intents (`select_class`, `rush`, `rend`); the server validates class and cooldown and applies everything. The server's `open_class_screen` signal carries the current class so reopening lands on its page, and the `class_confirmed` reply is the only thing that closes the picker.

## Persistence

- **Saved**: the class choice only, under `InfusedClass` on the player. Survives relog and respawn; travels with player data backups.
- **Transient**: stacks, stance windows, cooldown lanes, pending bonuses. Never saved; cleared on logout, respawn and class switch.

Safe to add to an existing save (players pick on next join). Removing the mod leaves the `InfusedClass` value behind harmlessly; reinstalling later reads it back.

## Licence

**All Rights Reserved** — see [LICENSE](../../LICENSE). Modpack inclusion with credit is permitted; redistribution and derivatives require permission. See [modpack-authors.md](modpack-authors.md) for how that applies to packs.
