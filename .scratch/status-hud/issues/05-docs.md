# 05: HUD guide page + glossary

Spec: `.scratch/status-hud/spec.md`

**What to build:** Players can read what the strip is: a guide page with the per-class roster (badge, what it counts, when it appears), the slot-name rule (SIG/ULT are names; Controls holds the real keys), one-decimal countdowns, the hiding rules, and pointers to `hud.enabled`/`hud.corner`. The glossary gains the strip's terms, and the chat-line entry stops claiming whole-second rounding.

**Blocked by:** 04 — documents the shipped strip.

**Status:** ready-for-agent

- [ ] `docs/guide/hud.md` exists, matches shipped behaviour, linked from README's guide list.
- [ ] Glossary gains Status HUD and Status snapshot terms with code anchors and `_Avoid_` lines; references ADR-0014.
- [ ] Chat line entry updated `%.0f` → `%.1f`; `grep '%.0f' GLOSSARY.md docs/` clean.

## Comments

None yet.
