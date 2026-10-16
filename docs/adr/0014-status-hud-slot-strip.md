# ADR-0014: The status HUD returns as the slot strip

Status: accepted

## Context

The mod's only HUD ever specced was `.scratch/passive-hud`: a Bulwark row
mimicking vanilla's effect strip with an icon, Roman-numeral stacks and a grace
bar. That plan was superseded when conditional-passive stacks became the vanilla
armor bar's job and the flat stat pipeline was deleted — since then nothing on
screen shows class state at all. Every lane timer, window countdown (Rush
stride, Rend empower, Impetus, Testudo, Aequilibrium, the Roulette outcome, All
In), Momentum stack plus grace, Streak count and stance lives in the
server-side transient runtime, and the client learns none of it; chat speaks
only at fire time, and in whole seconds. Playing any class well means counting
in your head.

## Decision

1. **Reinstate the HUD as a slot strip**, not an effect-row mimic: one vertical
   strip of vanilla-slot-style chips anchored to a screen corner (bottom-right
   by default; chat owns the left), labels on the inner side mirrored per
   corner, no backdrop. Badge boxes are one uniform width — sized once for
   three-character badges — at height 20; letters and numbers only in v1.
2. **Badges are slot and state names, never keys**: `SIG`/`ULT` for the two
   lanes; `MOM`, `STR`, `GLA`/`SCU`, `IMP`/`TES`, `AEQ`, `ALL`; and single
   initials for Roulette outcomes (never Zero). The strip never reads a
   KeyMapping, so a remapped key cannot make it lie.
3. **One snapshot at change points**: a new S2C `PlayerStatusPayload` carries
   the strip's state at send time; the server emits it when something a client
   cannot derive changed (fired ability, moved hit settle, moving decay tick,
   class switch, respawn, join) and stays silent otherwise. The client
   interpolates countdowns between snapshots.
4. **Rendering is a decision**: `StatusHud.rows(status, elapsed)` is pure and
   returns the strip's rows; the HUD element draws them. Hiding is the
   adapter's: no class, F1, any open screen.
5. **Countdowns go precise** everywhere, chat (`ChatLine.cooldown`) included:
   one decimal, `%.1fs`.
6. **Config is drawing only**: `hud.enabled` and `hud.corner` in the client
   config file stop or move the drawing and change nothing else.

## Consequences

- The client joins the state conversation for the first time: one more payload
  registered beside its siblings, one server-side last-sent map per UUID, and a
  client store holding one snapshot plus an interpolation anchor.
- `.scratch/passive-hud` points here as superseded; the effect-row approach is
  closed — no icons, no Roman numerals, no vanilla effect integration.
- The roster is data-driven per class, so a future ability (a real Juggernaut
  ultimate) adds a row instead of a branch; an icon pass later re-tightens the
  boxes to square slots — the uniform width is the v1 accommodation for
  three-character badges.
- Emission is guarded by a pure `movedSince` comparison, so rejected presses,
  spam swings and quiet ticks cost no packets.
