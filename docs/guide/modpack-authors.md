# Modpack authors

Infused Classes is **All Rights Reserved**, but the licence explicitly permits including it in a modpack under a few conditions. This page summarises the practical parts — the authoritative text is [LICENSE](../../LICENSE).

## What you may do

- **Include the mod in a pack** distributed through **CurseForge, Modrinth, or an official launcher with a distribution agreement**. Third-party hosting, mirrors and direct download links are not permitted.
- **Monetise the pack** only through CurseForge/Modrinth's standard systems (sponsored links, project-page banner ads). Any other monetisation needs written permission.
- **Create content** — videos, streams, screenshots, reviews — and monetise it normally. Credit is appreciated, not required, for content.

## What you must do

- Give **clear, prominent credit** in the pack description with:
  1. the mod name — **Infused Classes**
  2. the author — **hunter1712**
  3. a link to the mod's official exchange page.
- Don't name or brand the pack so it's confusable with Infused Classes.

## What you may not do

- Re-upload, mirror or rehost the jar outside the allowed platforms (includes file-sharing sites, Discord servers and direct download links).
- Release a fork, port, patch or derivative.
- Extract or reuse the mod's assets.
- Bundle a **modified** build into a pack (private edits are allowed for personal/private-server use only, never redistributed).

## Tuning it for your pack

No code changes are needed and no knobs exist — there is no config file. Everything pack-relevant is the install shape:

- Ship the jar matching your pack's game version (`+1.20.1`, `+1.21.1` or `+26.3`); all players plus the server run the same jar on the same version.
- Tell players an update may require a reselect (`/class select <player>`).

## Dependencies

- **Fabric Loader 0.19.5+**, **Fabric API** and the Java runtime your game version needs (17, 21 or 25) are required; nothing else.
- No custom blocks, items or worldgen, so it won't fight your world-gen or content mods.
- Safe to add to an existing save (players pick on next join).

## Quick facts

| | |
|---|---|
| Environment | Client **and** server (server-required) |
| Saved per player | Class choice (`InfusedClass`) |
| Transient | Stacks, stances, cooldowns, windows |
| Tested versions | MC 1.20.1, 1.21.1, 26.3 on Fabric, Loader 0.19.5+ |
