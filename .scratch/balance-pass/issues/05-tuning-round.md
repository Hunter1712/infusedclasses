# 05 — Tuning round

Status: done
Spec: `.scratch/balance-pass/spec.md`

## What

The first-cut table left classes outside the ±10% band (Juggernaut low, Gladius and
Gambler high). The knobs that fix it shape class identity, so the values were the user's
call, not the implementer's.

## Settled (2026-10-19)

- Metric: offense **and** defense measured on the same timeline (model v2); official band
  is ±10% around the five-row mean.
- Juggernaut: Burden swing **-0.5 → -0.4** (0.88 → 0.96).
- Gladius: glass tax **x1.1 → x1.2** (1.19 → 1.10; alacrity +0.1 was the runner-up at 1.06).
- Scutum: **unchanged** (0.92 with Testudo's cycle and the plate priced).
- Gambler: crit base **20% → 15%**, All In **x2.0 → x1.75** (1.12 → 1.09).

## After the decisions (done)

`model.py` recomputes the table (v2), the spec table is updated, the knobs are pinned in
`BalanceBudgetTest.theTuningRoundHoldsItsDeclaredRows`, copy regenerates from the
constants. Remaining: the in-game hand checks listed in `AGENTS.md`.
