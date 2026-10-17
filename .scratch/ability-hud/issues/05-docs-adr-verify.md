# 05: Docs, ADR and verification

Status: ready-for-human

## Context

ADR-0019 scrapped the HUD "pending a later implementation"; this is that implementation, with a new look and no config file. The repo's docs must move with it in the same change.

## Goal

- `docs/adr/0020-ability-hud-returns.md`: the reversal — icon stack at the left-centre, whole-second timers, ten generated sprites, no config file, transport rebuilt from the archived shape; record what carried over from ADRs 0014/0017/0018 and what changed.
- `GLOSSARY.md`: re-add the HUD vocabulary (status HUD, status snapshot, status sync, status store) and the new terms (icon stack, whole-second countdown), pointing at this lane; update the Decisions paragraph with 0020.
- `docs/guide/hud.md` returns (what the stack shows, the roster per class, whole-second reading, no config, hiding rules); `docs/guide/faq.md` and any other page that mentions the scrapped HUD gets the new state; check `README.md` and `docs/guide/*.md` for stale references.
- `CHANGELOG.md`: one entry under the unreleased section, house format.
- Verification: `gradle test` green, then `gradle deployClient`; in-game pass with `/class select <player>` for each class — lanes count down, riders appear and vanish on their boundaries, Momentum/Streak rows move, classless shows nothing, F1 and open screens hide the stack.

## Done when

Docs and glossary agree with the code, the ADR is accepted, and the in-game pass is reported in this ticket's Comments.

## Comments

Docs half done: `docs/adr/0020-ability-hud-returns.md` accepted, ADR-0019 marked superseded, glossary gains the Status HUD / Rider / Status snapshot / Status sync / Status store terms and the `0020` decision line, `docs/guide/hud.md` returns, `compatibility.md` and `README.md` point at it, and `CHANGELOG.md` carries the unreleased entry. Remaining: the in-game pass. Suggested script: `/class select <player>` for each class, then watch lanes count down, fire each ability, and check the rider boundaries (stride `+` drop, empower/IMP vanish, Testudo `2s` last second, Zero rider absent), Momentum and Streak rows appearing and dropping, classless showing nothing, F1 and screens hiding the stack.

Docs redone by 06 under ADR-0021 (0020 marked superseded; hud.md, glossary, CHANGELOG, compatibility and the spec carry the word look). Verification list updated: the stack is words only with no icon anywhere; pitch is 12 gui (row 10 + gap 2) with a single space between a word and its number (numbers ragged, no column padding); the Rush rider reads `5s` spent or loaded (no `+`); the Roulette rider prints the band word (`· Jackpot 8s`); Momentum reads `stacks · grace` and must keep refreshing its grace while you keep hitting a mob at 20 stacks — the cap re-stamp that used to freeze at `0s` — and must never print `0s`; the Rend window rider still vanishes on the spend; Testudo's last second reads `2s`. Added by the vocabulary sweep: the Rush picker card reads `Cleanses harmful effects and quickens you for 5s` (was `quickens stride`), and the words `stride`/`empower` appear in no identifier, comment, test or card line.
