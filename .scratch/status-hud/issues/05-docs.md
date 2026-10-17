# 05: HUD guide page + glossary

Spec: `.scratch/status-hud/spec.md`

**What to build:** Players can read what the strip is: a guide page with the per-class roster (badge, what it counts, when it appears), the slot-name rule (SIG/ULT are names; Controls holds the real keys), one-decimal countdowns, the hiding rules, and pointers to `hud.enabled`/`hud.corner`. The glossary gains the strip's terms, and the chat-line entry stops claiming whole-second rounding.

**Blocked by:** 04 — documents the shipped strip.

**Status:** done

- [x] `docs/guide/hud.md` exists, matches shipped behaviour, linked from README's guide list.
- [x] Glossary gains Status HUD and Status snapshot terms with code anchors and `_Avoid_` lines; references ADR-0014.
- [x] Chat line entry updated `%.0f` → `%.1f`; `grep '%.0f' GLOSSARY.md docs/` clean.

## Comments

Every roster and hiding claim on the page was checked against the shipped code: `StatusHud.rows` (roster, riders, `+`, dim/lit, Zero no-chip, `%.1fs` clamped), the element's hide rules and corner mirroring, and `StatusSync`/`StatusStore` for the snapshot wording; lane lengths cross-checked against `Cooldowns`. The page was verified against the pure decision the same way the tests pin it — the live rendering it documents is the one human in-game pass deferred from ticket 04. Grep confirmed `%.0f` remains only under `.scratch` (history stays); the Decisions roll-up in the glossary now records ADR-0014 too.
