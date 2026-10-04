# 02: Tier spend, keystone chain, result type

**What to build:** The unlock rules over the ranked data: tier thresholds at zero, five, ten, and twelve spent (global count while Defence is the only tree), parent edges only for attachments (Enduring and Hold the Line need Thick Skin, Rallying Roar needs Hold the Line), flat one-point ranks with afford checks, and the small unlock-result type replacing the English-string reason, formatted to tooltips client-side and chat server-side.

**Blocked by:** 01 (needs the ranked node data to validate against).

**Status:** done

- [x] Below-tier spends reject; keystone-chain skips reject (augment without Thick Skin, capstone without Hold the Line)
- [x] Thirteen-point rush path and twenty-point full clear both compute; Bulwark parity values assert per rank
- [x] Denied picks still explain themselves in chat via the existing server path

## Comments

CLOSED — implemented in `3c68c71`, full suite green. Chat path reuses the unchanged server denial flow with the new result-type wording (unit-covered by `describe` test).
