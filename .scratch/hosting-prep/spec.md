# Spec: Hosting Prep (Distribution + Docs, No Upload Yet)

Status: done

## Problem Statement

As the mod author I have a second Fabric mod that already hosts on Modrinth/CurseForge with a single project page, manual Alpha jars, in-repo player guide, and a restrictive rehost policy — but this repo is still manual-local builds only with a permissive license and no listing body, changelog, or guide. I want the listing and docs skeleton prepped now so a future upload is a copy-paste, without uploading anything yet and without paying for automation or a multi-version split early.

## Solution

Prep the future distribution and docs hosting skeleton mirroring the sibling mod pattern: single project page with a version-suffixed jar name reserved for listing time, server-required install contract, full in-repo player guide with listing body and changelog, and a restrictive license plus modpack policy. No upload, no CI publish plugin, no playtest server, no multi-module restructure, no icon art blocking.

## User Stories

1. As a future Modrinth visitor, I want one project page for all Minecraft versions, so that I pick the right jar without hunting across pages.
2. As a future CurseForge visitor, I want the same single-page listing with Alpha channel jars, so that both platforms agree.
3. As a player on 1.21.11, I want the Alpha jar name to carry its Minecraft version once listed, so that a later 26.x jar slots alongside it with zero rename.
4. As a local alpha tester today, I want local builds to keep the current short jar name, so that the dev-client deploy flow does not churn before listing.
5. As a server admin reading the listing, I want a clear server-required plus same-jar-on-client install contract, so that I do not file version-mismatch issues.
6. As a Juggernaut player reading the guide, I want a classes page describing Juggernaut with Rush engage plus Rend spender, so that I learn the kit without opening the game.
7. As a Juggernaut player reading the guide, I want a stances-passives page describing Momentum stacks plus Vigor HP-scaled pair with picker copy numbers, so that copy cannot drift from behaviour.
8. As a Legionary player reading the guide, I want Rotate plus Impetus flip buff plus Testudo catch plus Aequilibrium oath described with cooldowns and windows, so that the stance tradeoff and shared oath lane are visible up front.
9. As a new player, I want an installation page with Loader plus Fabric API plus Java 21 plus no-Forge/Quilt plus no-backport statements, so that wrong-environment reports close as wontfix.
10. As a player, I want a commands page documenting the gamemaster-only class wiring plus testing-only select reopen, so that I know what admins can do.
11. As a player, I want a configuration page (even if empty today), so that future knobs have a home.
12. As a player, I want a compatibility page stating Fabric-only and server-authoritative intent handling, so that I do not ask for Forge ports.
13. As a player, I want a version-policy page stating 1.21.11-only now with 26.x later as a separate branch, so that backport requests have one answer.
14. As a modpack author, I want a modpack-authors page stating platform-only distribution with no mirrors or direct-link rehosts and no forks or asset reuse, so that the rules are unambiguous.
15. As a modpack author, I want private-server edits allowed but never redistributed stated explicitly, so that testing tweaks do not become redistributions.
16. As a player, I want a faq page answering wipes plus resets plus class reselect after updates, so that alpha-breakage expectations are set.
17. As a listing reader, I want the Modrinth/CurseForge body pre-written from the repo source of truth, so that both platforms show identical text.
18. As a returning player, I want a Keep-a-Changelog file starting at the current alpha version, so that future jars have a place to note wipes and kit changes.
19. As the author, I want the permissive license replaced with the sibling restrictive license before listing, so that rehost bans are enforceable from the first upload.
20. As the author, I want the README alpha contract plus listing blurb updated to match the new license and install contract, so that the repo does not contradict the future listing.
21. As the author, I want no upload performed by this work, so that prepping files never creates a public listing early.
22. As the author, I want icon art explicitly deferred and non-blocking, so that missing art never delays docs or license prep.
23. As a future multi-version maintainer, I want the single-version layout kept with no common split now, so that I do not pay the sibling four-module tax before the 26.x branch exists.

## Implementation Decisions

- Seams under test (highest possible, existing preferred — no new Java seams):
  - Seam 1 — repo metadata decision: license text plus README alpha contract plus draft listing blurb must agree (restrictive license, server-required install, Alpha manual-only, wipe expectations). Verified by reading rendered text, not by JUnit — no gameplay module is touched.
  - Seam 2 — docs corpus decision: guide set plus listing body plus changelog must exist with the agreed page list and must use domain glossary vocabulary (Class, Conditional passive, Momentum, Vigor, Rush, Rend, Legionary, Gladius, Scutum, Rotate, Impetus, Testudo, Aequilibrium, Class choice, Picker screen, Card lines, Picker copy, Class command). Verified by corpus presence and terminology, not by game bootstrap.
  - Ideal seam count is zero Java seams: this work adds no decide-then-apply modules and reopens no ADR seams (0001 pure/adapter split, 0002 choice/transient split, 0003 deferred lookup all untouched).
- Distribution shape: one project page on both platforms, Alpha channel, manual jar upload at listing time only. Artifact carries Minecraft-version suffix at listing time; local dev builds keep the short name until then.
- Install contract: server-required plus same jar on both sides, current Loader floor plus Fabric API plus Java 21, single Minecraft version locked, no Forge/Quilt, no backports in 0.x.
- Docs set: installation, classes, stances-passives, commands, configuration, compatibility, version-policy, modpack-authors, faq, plus listing body plus changelog. Guide pages generate no numbers of their own — every number cited comes from the existing picker copy generation so behaviour and docs cannot drift.
- Listing body is a copy-paste of the repo source of truth; the repo remains canonical per the README alpha contract.
- Changelog follows Keep-a-Changelog starting at the current alpha; each future 0.x entry must call out wipes or reselects.
- License migration: single-author history (no external consent needed) allows direct replacement of the permissive text with the sibling restrictive text plus README update plus modpack policy page. Private-server edits allowed, redistribution banned; packs only via official platforms and launchers.
- No automation: existing build-only CI stays as-is; no publish plugin, no secrets, no release workflow in this work.
- No restructure: single-version layout kept; multi-module common split deferred until the 26.x branch actually exists.

## Testing Decisions

- What makes a good test here: external textual agreement, not implementation internals — the listing body, guide pages, changelog, and README must say the same version, loader floor, install contract, and wipe policy with glossary terms spelled correctly. No game bootstrap, no graphics objects, no player or level fixtures.
- Tested: presence of the full guide page list; listing body mentions Alpha plus manual install plus server-required plus wipe policy; changelog has an Unreleased/current-alpha section; license text is the restrictive form and README no longer claims permissive terms; no upload artifact or workflow change.
- Prior art: pure decision tests for card presentation and picker copy (numbers-cannot-drift rule); layout containment tests for the picker. This spec follows the same spirit at the docs layer: assert what the reader sees, not how files are templated.
- Not tested: actual Modrinth/CurseForge upload flow, rendered markdown pixel checks, icon art, gradle publish wiring — manual verification at listing time.

## Out of Scope

- Actual upload or creation of Modrinth/CurseForge listings (explicitly deferred by author).
- Release automation: publish plugins, CI upload jobs, GitHub Releases workflow, API tokens.
- Playtest server hosting: dedicated server provisioning, world hosting, allowlists.
- Multi-version restructure: common module split, per-version source sets, backports.
- Icon art, screenshots, videos, or listing imagery.
- Balance, kit, picker, or networking changes; no Java behaviour changes in this work.
- Web hosting patterns from the sibling Flask project (containers, WSGI servers, database volumes, TLS) — not applicable to a Fabric jar.
- Data migration between alpha jars; wipes remain allowed per the alpha contract.

## Further Notes

- Grilling settled scope as distribution plus docs only, template as the sibling Fabric mod, single-jar manual until beta, license flip to restrictive, no upload gate — prep files now, list later.
- Sibling reference facts: multi-module layout with empty publish block, version-plus-Minecraft suffix jars, single page with per-version jar pick, in-repo guide with no Pages site, restrictive license with platform-only plus no-mirror plus no-fork rules, server-authoritative with vanilla nameplate sync.
- Relicense is clean: commit history is single-author plus one local alias, so no contributor consent sweep is needed; still, the flip should land as its own commit with the README update for a clear paper trail.
- Follow-up at listing time (not this work): flip the jar name to the suffixed form, paste the prepped body, upload the Alpha jar manually, then add icon art without touching docs.
