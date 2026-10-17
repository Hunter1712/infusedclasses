# 06: Text stack rework and the grace stamp

Status: done

## Context

ADR-0020's icon stack shipped, then failed the eye test one look later: with the mod's own 16×16 art, a glance could not tell Rush from Rend or Rotate from Aequilibrium, and the badge vocabulary (`IMP`/`TES`/`AEQ`/`ALL`, the outcome initials, the stride `+`) made each row speak two languages at once. A browser prototype (six variants, `build\hud-text-prototype.html`) was pictured before any code moved; the word look won every row, and the window riders proved unambiguous as bare dotted counts. Separately, an in-game report — "momentum 20 ticks down to 0s always even after hitting a mob" — traced to a real gap: a hit at the stack cap re-stamps the grace server-side without moving the count, so `movedSince` saw no move, no snapshot was sent, and the client's interpolated grace ran to a stale `0s` and stayed there mid-combat.

## Goal

- `StatusHud.rows` changes shape: words instead of icons and badges, `Chip(label, value, lit, rider)`, each chip drawn as its word, one space and its number — no label column (the padded first cut read as tab stops); riders are bare dotted numbers except the Roulette band word; the Rush `+` marker is dropped; the Momentum grace number prints only while live (never `0s`).
- `StatusHudElement` draws text only at pitch 12 (row 10 gui, gap 2), no blit.
- `PlayerStatusPayload` gains `momentumGraceStampMs`, read from `PlayerRuntime.graceStamp` (backed by `PassiveStacks.graceStamp`); `movedSince` compares it, so a cap re-stamp emits exactly one snapshot.
- Delete the ten icon PNGs, `tools/generate-hud-icons.ps1` and `textures/gui/ability/`; no GUI art ships.
- Docs: ADR-0021 (ADR-0020 marked superseded), `docs/guide/hud.md`, GLOSSARY (Status HUD, Rider, Status snapshot, Decisions), CHANGELOG, compatibility, spec; `classes.md` and the glossary speak of the Rush window and the Rend window.

## Done when

`gradle test` is green with the reworked `StatusHudTest` (words, no `+`, no `0s`) and the new cap re-stamp regression test; `gradle deployClient` refreshes the live jar; ticket 05's in-game pass confirms.

## Comments

Done: the rework, the grace stamp, the art deletion and the docs landed together. The prototype was fixed too (the Rush rider prints `5s`) and its baseline icon variant now reads from the branch's own copy of the deleted PNGs (`build/proto-icons/`), so the comparison still renders.

Prototype branch: `prototype/hud-text-words`, commit `f54dc00` — carries `build/hud-text-prototype.html` (six variants) and `build/proto-icons/` (the ten deleted PNGs).

Follow-up from the first in-game look: the padded label column read as tab stops (`All In   34s`), so the column was cut in the same pass — each chip now draws as its word, one space and its number (`Roulette 13s` over `All In 34s`). Code, prototype, ADR-0021, guide, glossary, spec and the example entry above all say one space now.

Vocabulary sweep (same review round): `stride` and `empower` left the code — `SPEED_BONUS`/`SPEED_ID` (`infusedclasses:rush_speed`), `RendAbility.bonusFor`, `peekRendBonusStacks`/`consumeRendBonus`, every comment, test names, the Rush picker copy (`quickens you for 5s`, was `quickens stride`) and the prototype's data keys and labels. ADR-0021 point 7 and a consequence record it; the prototype branch was amended again.
