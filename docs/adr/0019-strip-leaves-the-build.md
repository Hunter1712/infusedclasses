# ADR-0019: The strip leaves the build

Status: superseded by ADR-0020 (the left-centre icon stack), then ADR-0021 (the stack speaks words)

## Context

Three HUD designs ran their course — ADR-0014's slot strip, ADR-0017's pills,
ADR-0018's bare zone stack — and every look rebuild cost the same price: the
draw half kept failing the eye test while the rows decision and the protocol
kept changing shape with it. Four builds deep, the strip still did not sit
right on screen. Rather than build a fifth look on the old vocabulary, the
leadership decided the whole arrangement should be taken apart and rebuilt
later from scratch, on its own lane.

## Decision

The status HUD is scrapped root and branch, pending a later implementation:

1. **The draw layer goes**: the `StatusHud` rows decision, the
   `StatusHudElement` client element and its `HudElementRegistry` wiring.
2. **The client config file goes** with the only knobs it ever carried
   (`hud.enabled`, `hud.corner`, `hud.scale`); `docs/guide/configuration.md`
   and `docs/guide/hud.md` go with it.
3. **The snapshot protocol goes**: `PlayerStatusPayload`, `StatusSync`, the
   `StatusStore` client cache, the server tick post-decay emission, the
   join/respawn/logout emission, the post-receiver emissions at every intent
   and the melee settle's emission in `settleAttackerHit`. A player's live
   state stays server-side in the transient runtime; the client learns
   nothing of it again until the reimplementation rebuilds a transport.
4. **Words stay decided already**: `AbilityOutcome.onCooldown` keeps its
   line-less silence and its machine-readable `remainingMs` (ADR-0017
   carry), and `ChatLine.cooldown` remains the `/class info` lane shape with
   the one-decimal rounding. If the reimplementation wants a different clock
   split, it reopens that with its own decision; nothing depends on it here.
5. **History is kept, not lived in**: the ADRs 0014/0017/0018 and the
   prototype branch `prototype/strip-goes-bare` record what was learned; the
   landing state of the whole chain is pinned in git (the WIP commit that
   carries the bare-zone build, tagged in the log by its message) so a
   reimplementation starts from the archive, not from memory.

## Consequences

- A cooldown press announces nothing and the screen shows nothing: a player
  tracks lanes through `/class info`, fire lines and their own pace. That is
  the price of the clean slate, and reimplementation may revisit ADR-0017's
  silence when no strip carries the countdown.
- The client no longer holds any second copy of gameplay state; the server
  sends gameplay-signal payloads only (picker open, pick confirm), and no
  per-tick background work consults the HUD's clock anymore.
- No config file exists: the build ships zero player-editable knobs until
  the reimplementation decides its surface again.
- The `.scratch/status-hud` lane closes as superseded by this decision, and
  `.scratch/passive-hud`'s effect-row archive stays superseded exactly as it
  was.
