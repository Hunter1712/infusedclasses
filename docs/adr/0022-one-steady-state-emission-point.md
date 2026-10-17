# ADR-0022: One steady-state emission point

Status: accepted

## Context

ADR-0020 point 4 rebuilt the transport and enumerated the change points that must call
`maybeSend`: the intent receivers post-branch, the hit settle, the post-decay tick and the
accepted switch, plus the forced join and respawn sends. Seven call sites lived in prose;
ADR-0021 point 7 carried the list unchanged.

Two receivers on that list were exactly where the classless leak lived (round-11 candidate
5): each looked the runtime up before the press could know it had nothing to reject, so the
enumeration had become a place for bugs to hide rather than a coverage guarantee. And the
tick already rebuilds and compares for every classed player every tick — `movedSince` is
the guard — so an immediate call can only buy latency, never coverage: a call missed
anywhere is at most one tick (50 ms) late, never stale.

## Decision

The server tick is the one steady-state emission change point:

1. **The post-branch emissions die**: the Rush receiver, the Rend receiver, the hit settle
   and the accepted switch no longer call `maybeSend`; the tick's rebuild-and-compare
   carries every one of them within 50 ms.
2. **The forced sends stay**: join and respawn keep `sendNow` (a client learns its class
   from a known state, classless included); logout keeps `forget`.
3. **Silence stays free and exact**: a quiet tick rebuilds and compares and sends nothing;
   a press, a spend, a decay, a switch and a settle all move a compared field and send on
   the tick that sees them.
4. **The classless guarantee holds trivially**: with one emitter, no emission path can
   materialise transient state just to send — the def-guarded lookups (ADR-0005) and the
   one emission home are the same seam.

## Consequences

- One steady-state call site replaces four: a new reason to emit adds no call to forget,
  and `maybeSend`'s only caller is the tick.
- Emissions at press, settle and switch become tick-latent: at most 1 tick, 50 ms, in
  exchange for the whole class of missed-call bugs. The stack still refreshes at 20 Hz;
  `.scratch/ability-hud` ticket 05's in-game pass owns confirming it feels live.
- ADR-0020 point 4's change-point carry and ADR-0021 point 7 are superseded on those
  points; their transport, clock and hiding decisions live on as cited.
