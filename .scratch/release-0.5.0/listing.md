# Release 0.5.0 — upload packet

Status: GitHub release published; Modrinth and CurseForge uploads pending.

## Artifact

- File: `build/libs/infusedclasses-0.5.0+1.21.11.jar`
- SHA-256: `933c6de2ab602af9432e4026e94b34d363837b0a55530decdb6e19433db9ae3a`
- Size: 188,866 bytes
- Internal `fabric.mod.json` version: `0.5.0+1.21.11`
- Tests: 276 passed, 0 failed (`./gradlew clean build`)

## Shared fields

| Field | Value |
|---|---|
| Name | Infused Classes |
| Summary | Character classes for Minecraft (Fabric): Juggernaut, Legionary and Gambler, each with abilities, stances and crit rules. Server + client install. |
| Project type | Mod |
| License | All Rights Reserved (link the repo `LICENSE`) |
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Environment | Client **required**, server **required** (same jar both sides) |
| Dependency | Fabric API (`required`) |
| Version name | `0.5.0+1.21.11` |
| Release channel / type | Beta |
| Source | https://github.com/Hunter1712/infusedclasses |
| Issues | https://github.com/Hunter1712/infusedclasses/issues |
| Guide / wiki | https://github.com/Hunter1712/infusedclasses/tree/main/docs/guide |
| Changelog | https://github.com/Hunter1712/infusedclasses/blob/main/CHANGELOG.md |

Body: paste `PROJECT_DESCRIPTION.md` verbatim.

## Modrinth

1. Create the project at https://modrinth.com/mod/create with the shared fields.
2. Slug: `infusedclasses` (free as of 2026-10-20).
3. Categories: **Adventure**, **Equipment**, **Game Mechanics**.
4. Links: source, issues, wiki (guide), changelog is not a Modrinth link field — it is in the body.
5. Upload version `0.5.0+1.21.11`, channel **Beta**, game version **1.21.11**, loader **Fabric**.
6. Add Fabric API as a required dependency in the version form.
7. Upload the artifact above; confirm the SHA-256 on the version page.

## CurseForge

1. Create the project at https://authors.curseforge.com/ with the shared fields.
2. Categories: **Adventure and RPG**; add **Magic** only if the closest-fit rule needs a second slot. CurseForge has no exact Equipment/Game Mechanics category.
3. License: **All Rights Reserved**.
4. Upload file `0.5.0+1.21.11`, release type **Beta**, game version **1.21.11**, mod loader **Fabric**.
5. Relations: **Fabric API** as a required dependency.
6. A new project can sit in moderation review before the page is public; that delay is not a release failure.

## After both uploads

- Verify Modrinth: `https://api.modrinth.com/v2/project/infusedclasses/version` returns `0.5.0+1.21.11` with `version_type: beta`, game version `1.21.11`, loader `fabric`.
- Verify CurseForge: the public page lists the file with release type Beta.
- Commit the live listing links into `README.md`, `PROJECT_DESCRIPTION.md` and the guide pages that still say "future manual uploads" / "no upload yet".
- Run the in-game checks owed by `.scratch/audit-fixes-02/spec.md`: kill-move matrix, Hard/Easy Gladius settle, `/class set` feedback, picker footer, cooling low-stack press, `latest.log` read. The cold `select_class` packet check and the raised-shield check remain known debt.
- A blocker found after upload means **0.5.1** plus disabling 0.5.0 on both platforms; a minor issue means 0.5.1 later and 0.5.0 stays listed.

## Checklist

- [x] Clean build, 276 tests green
- [x] Jar renamed `+1.21.11`, internal version matches
- [x] Deployed to live `1.21.11` instance (old short-named jar removed)
- [x] Packaging commit, `main` pushed, CI green
- [x] Annotated tag `v0.5.0`, GitHub release with jar and SHA-256
- [ ] Modrinth project created and 0.5.0 uploaded
- [ ] CurseForge project created and 0.5.0 uploaded
- [ ] README / PROJECT_DESCRIPTION links updated after pages are live
- [ ] In-game checks run and `latest.log` read
