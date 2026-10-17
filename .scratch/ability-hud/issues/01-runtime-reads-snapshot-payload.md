# 01: Runtime reads, snapshot payload and protocol round-trip

Status: done

## Context

The client can only draw what the server tells it: lane cooldown remainders, window remainders and load flags, Momentum stacks plus grace, stance, Streak, and the live Roulette outcome all live in `PlayerRuntime`. ADR-0019's scrap deleted the snapshot payload and the runtime's remaining-time reads; both return here, unchanged in shape.

## Goal

- Re-add the per-window remaining-millis verbs on `PlayerRuntime` (`rushWindowRemaining`, `rendWindowRemaining`, `flipWindowRemaining`, `rouletteWindowRemaining`, `aequilibriumWindowRemaining`, `allInWindowRemaining`), the private `Window.remainingMs`, and `PassiveStacks.graceRemaining` plus its runtime forward `graceRemaining(passive, now)`. All return 0 when nothing is live / no stacks are held.
- Restore `PlayerStatusPayload`: record, `TYPE` (`infusedclasses:player_status`), hand-rolled `STREAM_CODEC`, builder `of(classId, runtime, now)` reading only runtime verbs, and `movedSince(prev)` comparing discrete fields only (never raw remainders, which decay).
- Register the S2C type in `InfusedClasses.registerPayloads`.
- Extend `ProtocolTest` with the codec round-trip, null stance/outcome included.

## Done when

`PlayerStatusTest` pins builder reads at armed/ready/live/spent boundaries and `movedSince` for every discrete move plus quiet time; `ProtocolTest` round-trips; `gradle test` is green.

## Comments

Implemented: the six `*WindowRemaining` reads, `Window.remainingMs`, `PassiveStacks.graceRemaining` and its runtime forward, the `PlayerStatusPayload` record with codec and `of`/`movedSince`, and the S2C registration. `PlayerStatusTest` and `ProtocolTest` cover it; `gradle test` green.
