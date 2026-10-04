# 02: Longer defence-only spine with single War Cry and Enduring Skin

**What to build:** A Juggernaut spine about twice as long as today that alternates blocks of small defence stat nodes with big passive/upgrade stops, gated top-to-bottom so stat blocks cannot be skipped: early stats into Thick Skin, more stats into the single Resistance War Cry upgrade, more stats into the Enduring Skin capstone that raises the Thick Skin cap and decay. The removed potency fork and its exclusivity rule are gone; per-node stat values are shrunk so the full build lands at vanilla-plus rather than godmode.

**Blocked by:** None (can start immediately; verifiable within the legacy point budget via commands and the existing panel).

**Status:** ready-for-agent

- [ ] Full spine spendable end-to-end through click and command fallback with server-side validation
- [ ] Staggered spend gates reject skipping (stat block before each big stop is mandatory)
- [ ] Only one War Cry upgrade exists and it adds Resistance with the longer duration; no fork choice remains
- [ ] Enduring Skin measurably raises the stack cap and lengthens decay in the stack machine
- [ ] Filler nodes are defence-only and reduced in value; full-build totals match the spec budget
- [ ] Pure-logic tests cover gate order, respec refund, stack cap/decay with capstone, single-upgrade effects, and serialization that drops removed nodes
- [ ] Progress survives death, relog, and respawn with stacks still wiped on death
