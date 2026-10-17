# ADR-0020: The ability HUD returns as the left-centre icon stack

Status: superseded by ADR-0021 (the stack speaks words)

## Context

ADR-0019 scrapped the whole status HUD chain — the strip, the config file, the
snapshot payload and the sync — and said the reimplementation would decide its
surface again on its own lane. The reimplementation arrived with a concrete
reference: a vertical stack of ability icons on the left of the screen, each
icon followed by its countdown. The lessons from ADRs 0014/0017/0018 narrowed
what could be reused: the rows decision (when to show what, and the rider and
window vocabulary) never failed a build; every failed half was the look and
the clock's presentation.

## Decision

The HUD returns, rebuilt from the archived shape with a new surface:

1. **The look is an icon stack at the left-centre**: one row per chip family,
   a 16x16 icon first, its number after it, riders on their lane's row. The
   block is anchored 4 gui from the left screen edge and vertically centred.
   No fills, no frames: icons and shadowed text only. Ten icons are generated
   (`tools/generate-hud-icons.ps1`) into `textures/gui/ability/`.
2. **The clock is whole seconds, rounded up** (`70s`, `9s`, `1s`): a live chip
   never reads zero. A ready lane draws its icon dimmed with no number, so the
   missing number is what marks it ready — ADR-0017's dim/bright split carried
   onto icons. `/class info` keeps its one-decimal line (ADR-0019 point 4).
3. **Badges change sides**: anchored chips (stance, lanes, Momentum, Streak)
   wear icons instead of `SIG`/`ULT`/`GLA`/`SCU`/`MOM`/`STR`; riders keep the
   badge vocabulary (`IMP`, `TES`, `AEQ`, `ALL`, Roulette initials) because a
   badge is a word substitute that art cannot carry for six outcomes. Badges
   remain slot and state names, never keys.
4. **The transport is rebuilt from the archive**: `PlayerStatusPayload` with
   its `movedSince` guard, `StatusSync` at the same change points (intent
   receivers post-branch, hit settle, post-decay tick, accepted switch,
   forced join and respawn), the client store, and the runtime's window and
   grace remainder reads. No config file exists: the anchor, the pad and the
   hide rules are constants in the element.
5. **Rendering stays a decision**: `StatusHud.rows(snapshot, elapsed)` returns
   the whole stack as pure rows; the element draws it and owns hiding (no
   class, F1, any screen, no snapshot).

## Consequences

- The client holds one snapshot copy again, interpolated from its receipt
  time; the server sends a packet only when `movedSince` says something a
  client cannot derive moved, so a quiet second costs nothing.
- Icon art becomes a maintained asset: retuning an icon is a script rerun,
  never a hand-edit of a PNG.
- ADR-0019's zero-knobs stance holds: no config file, no toggle key, no
  corner choice; the left-centre anchor is the only layout, and changing it
  is a code change with a decision, not a setting.
- The retired badge names stay in no vocabulary: the stack speaks in its
  icons and rider badges, fire lines and `/class info` keep the words.
- The `.scratch/ability-hud` lane owns the spec; the `.scratch/status-hud`
  lane stays closed as superseded by ADR-0019.
