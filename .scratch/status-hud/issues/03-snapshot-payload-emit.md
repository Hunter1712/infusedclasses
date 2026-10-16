# 03: Snapshot payload + change-point emission

Spec: `.scratch/status-hud/spec.md`

**What to build:** The client hears about state exactly when state moved: the snapshot record travels as its own S2C payload, and a server-side sync helper keeps the last-sent snapshot per player and sends only when `movedSince` says something discrete changed — at the decided change points (fired ability, moved hit settle, moving decay tick, accepted class switch, respawn, join). Join and respawn force one so a client always learns its class, classless included. A small client store keeps the snapshot and its receipt time for the element to read.

**Blocked by:** 01 — the record and its guard.

**Status:** ready-for-agent

- [ ] Payload registered beside its siblings; codec round-trip pinned in the protocol test (every field, boundary values).
- [ ] Emission verified live with a temporary log (removed before done): a snapshot on join, each fire, a moving hit, stack decay, class switch, respawn — and nothing between them; a rejected press sends nothing.
- [ ] Client store overwritten per payload, cleared on client disconnect; last-sent map removed on server disconnect and re-seeded on the shared-UUID respawn.
- [ ] Classless join sends an empty-class snapshot (the client hides by fact, not by absence).
- [ ] `gradle test` green.

## Comments

None yet.
