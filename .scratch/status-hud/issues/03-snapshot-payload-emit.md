# 03: Snapshot payload + change-point emission

Spec: `.scratch/status-hud/spec.md`

**What to build:** The client hears about state exactly when state moved: the snapshot record travels as its own S2C payload, and a server-side sync helper keeps the last-sent snapshot per player and sends only when `movedSince` says something discrete changed — at the decided change points (fired ability, moved hit settle, moving decay tick, accepted class switch, respawn, join). Join and respawn force one so a client always learns its class, classless included. A small client store keeps the snapshot and its receipt time for the element to read.

**Blocked by:** 01 — the record and its guard.

**Status:** done

- [x] Payload registered beside its siblings; codec round-trip pinned in the protocol test (every field, boundary values).
- [ ] Emission verified live with a temporary log (removed before done): a snapshot on join, each fire, a moving hit, stack decay, class switch, respawn — and nothing between them; a rejected press sends nothing.
- [x] Client store overwritten per payload, cleared on client disconnect; last-sent map removed on server disconnect and re-seeded on the shared-UUID respawn.
- [x] Classless join sends an empty-class snapshot (the client hides by fact, not by absence).
- [x] `gradle test` green.

## Comments

Landed `StatusSync` (server) and `client/StatusStore` beside the existing payloads: the payload is registered in
`InfusedClasses.registerPayloads` next to its siblings, and `ProtocolTest` pins the `infusedclasses:player_status`
channel, the five distinct namespaced channels, and codec round-trips of a full snapshot and a classless empty one
(the codec is `FriendlyByteBuf`-typed, so the tests wrap `Unpooled.buffer()`). Change points wired: JOIN and
AFTER_RESPAWN force `sendNow` (classless included), DISCONNECT and the respawn's stale half call `forget`, the server
tick calls `maybeSend` after the decay-and-apply pass, both intent receivers and the melee settle call `maybeSend`
after their branch (a rejected press and a spam swing send nothing), and an accepted `ClassSwitch` sends once.
Client side: the receiver writes `StatusStore.accept(payload, now)` and `DISCONNECT` clears the store; `StatusStoreTest`
pins whole-overwrite and clear. The classless-empty join is pinned unit-level (`classlessCarriesTheEmptyClassId` plus
the classless codec round-trip) and the unconditional JOIN `sendNow` reads directly in `ServerEvents`.

The live emission log checkbox stays unticked: manual live emission pass pending (no in-game run in this session).
Verified: `gradle test`, 221 tests across 26 suites, 0 failures.
