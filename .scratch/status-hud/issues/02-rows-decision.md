# 02: Rows decision + one-decimal countdowns

Spec: `.scratch/status-hud/spec.md`

**What to build:** Given the last snapshot and the millis since it arrived, a pure decision lays out the whole strip as rows: which chips a class shows, in what order, their badges and labels, which are dim (ready) versus lit (cooling or live), countdowns as `%.1fs`, and the `+` marker on the stride rider while the engage bonus is unspent. Zero renders no chip; a classless player no rows. Chat's shared cooldown line drops to the same one-decimal precision, so chat and the strip can never disagree about how a countdown reads.

**Blocked by:** 01 — the snapshot's field set.

**Status:** ready-for-agent

- [ ] Rows for all three classes at boundary snapshots: lane rows always present (dim ready, lit cooling); riders exactly while their window is live; stride keeps counting with `+` dropped after the spend; empower/IMP riders vanish on spend; MOM carries stacks plus grace; STR only above zero; Zero and classless render nothing.
- [ ] Countdown formatting pinned at 10_000 / 9_999 / 0 ms, clamped at zero; elapsed past a window drops its row.
- [ ] Labels read from the class definition and ability constants (no second copy to drift); badges from the fixed vocabulary.
- [ ] `ChatLine.cooldown` reads `%.1fs`; any old whole-second pin updated.
- [ ] `gradle test` green; the decision reads only the snapshot, elapsed and pure class data.

## Comments

None yet.
