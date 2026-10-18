# 02 — The silent reject is one shape

Status: done

## Context

`AbilityOutcome` carries `remainingMs`, built by six per-ability `onCooldown` factories (plus
`Result` wrappers in Rend/Rotate/Roulette) for the wrong-class vs on-cooldown distinction — a
distinction no production path reads since ADR-0017 silenced both; only tests read the
number. Round-12 candidate 2.

## Deliverable

- `AbilityOutcome(boolean fired, ChatLine announcement)`; `fired` / `rejected` / `fizzled`
  stay, `onCooldown` dies.
- The six fire paths return `rejected()` on cooldown; the `Result` factories collapse
  `wrongClass`/`onCooldown` into one `rejected()`.

## Done when

- No outcome carries a remainder; the five test readers assert the silence (`!fired`, no
  line) or re-pin the lane through `state(now)`.
