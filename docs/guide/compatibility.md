# Compatibility

## Supported versions

One project page, one jar per game version. Download the jar whose `+<mc>` suffix matches your game version.

| Mod loader | Minecraft | Java | Jar |
|------------|-----------|------|-----|
| Fabric | 1.20.1 | 17+ | `infusedclasses-<mod>+1.20.1.jar` |
| Fabric | 1.21.1 | 21+ | `infusedclasses-<mod>+1.21.1.jar` |
| Fabric | 26.3 | 25+ | `infusedclasses-<mod>+26.3.jar` |

Each jar declares a single-version range (`~<mc>`) in its own `fabric.mod.json`, so a jar for the wrong version is rejected at launch rather than misbehaving. No Forge/Quilt support. **1.21.11 is no longer supported** — its jar does not exist downstream of that release; move to the nearest supported version — see [version-policy.md](version-policy.md).

## Dependencies

| Dependency | Required? | Notes |
|------------|-----------|-------|
| **Fabric Loader** | Yes | `0.19.5` or newer |
| **Fabric API** | Yes | Any build matching your Minecraft version |
| Anything else | No | No hard dependencies |

## Client / server

| Side | Required | Why |
|------|----------|-----|
| Server | **Yes** | Class choice validation, ability gating and arming, hit planning and settling, stance auras — all server-side and server-authoritative |
| Client | **Yes** (supported configuration) | Picker screen, status HUD (left-centre text stack), keybind intents (`R` Signature, `V` Ultimate), pick confirm handling |

The client only sends intents (`select_class`, `rush`, `rend`); the server validates class and cooldown and applies everything. The server's `open_class_screen` signal carries the current class so reopening lands on its page, and the `class_confirmed` reply is the only thing that closes the picker. The `player_status` snapshot drives the status HUD: the server sends one only when something the client cannot derive moved, and the client interpolates the countdowns from the receipt time.

The wire format, save-data key, gameplay rules and numbers are identical on every supported version; only the adapters that touch Minecraft (mixin targets, networking codecs, damage APIs, client widgets) are version-specific. A server and client of the same version are required — the protocol and gameplay are version-matched.

## Persistence

- **Saved**: the class choice only, under `InfusedClass` on the player. Survives relog and respawn; travels with player data backups.
- **Transient**: stacks, stance windows, cooldown lanes, pending bonuses. Never saved; cleared on logout, respawn and class switch.

Safe to add to an existing save (players pick on next join). Removing the mod leaves the `InfusedClass` value behind harmlessly; reinstalling later reads it back.

## Licence

**All Rights Reserved** — see [LICENSE](../../LICENSE). Modpack inclusion with credit is permitted; redistribution and derivatives require permission. See [modpack-authors.md](modpack-authors.md) for how that applies to packs.
