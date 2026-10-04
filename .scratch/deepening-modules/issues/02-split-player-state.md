# 02: Split per-player state at its pure/live seam

**What to build:** The per-player state class currently holds a pure validation path and a live transient-stack path side by side with no seam between them. Split them. One module owns the chosen class, validation, and the NBT round-trip, and contains no clock reads. The other owns stacks, the cap, decay and the grace period, with the clock injected. Since the transient half is no longer reachable from the player, the server entry holds it per player and owns its lifetime — creation, respawn handling and teardown.

From the player's side nothing changes: the class survives relog and respawn, stacks stay transient, and a respawn does not copy stacks.

**Blocked by:** 01 — Passive effect as data, stacks-to-attribute as a pure decision

**Status:** ready-for-agent

- [ ] The module holding the chosen class contains no clock reads and no transient state.
- [ ] The module holding stacks owns the cap, decay and grace period, and takes its clock as a parameter.
- [ ] The player holds the choice module; the server entry holds the transient stacks module per player.
- [ ] Respawn copies the choice only; transient stacks start empty on the new player.
- [ ] Nothing transient is written to persistence or copied on respawn.
- [ ] Creation, respawn and teardown of transient state all live in the server entry, in one place.
- [ ] The single-method accessor interface is narrowed to what the mixin genuinely needs, or deleted. Deletion is the default.
- [ ] The passive effect module from ticket 01 continues to read the stacks module and apply its answer unchanged.
- [ ] Existing stack, decay, grace-period, respawn and persistence assertions pass with only import and call-site changes — no expectation is rewritten.
- [ ] The NBT key and what persists are unchanged.