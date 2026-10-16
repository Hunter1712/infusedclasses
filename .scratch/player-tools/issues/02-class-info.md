# 02: /class info + open /class root

Spec: `.scratch/player-tools/spec.md`

**What to build:** Every player can ask about themselves: bare `/class` and `/class info` speak for the invoking player — class display name, one line per lane with its remaining time in one decimal (`Rush: 18.4s`, `Rend: ready`), and the Legionary's stance. No class → one plain line. Opening the root is the risky half: the old gamemaster gate moves onto `set` and `select` (which keep it, `select` still testing-only), and the info lines are a pure decision so they cannot drift from the lane reads.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Per-class line goldens at ready/cooling boundaries with `18.4s`-style pins; stance line present for Legionary only; no-class line; unknown/missing class safe.
- [ ] Manual: `/class` and `/class info` for each class and for a classless player; console gets vanilla's player-required failure.
- [ ] A non-op's tree shows `class` and `info` only; `/class set` and `/class select` unchanged and still refused for a non-op.
- [ ] `gradle test` green.

## Comments

None yet.
