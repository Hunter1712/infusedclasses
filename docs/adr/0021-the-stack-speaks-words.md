# ADR-0021: The stack speaks words

Status: accepted

## Context

ADR-0020 brought the HUD back as the left-centre icon stack and generated ten
16x16 icons for it. The reference look that justified the icons did not survive
contact with the mod's own art: a glance could not tell Rush from Rend or Rotate
from Aequilibrium — the column read as ten similar plates, and the numbers
after them did all the work. Riders kept badges (`IMP`, `TES`, `AEQ`, `ALL`, the
Roulette initials) beside icons, so each row spoke two languages at once.
Meanwhile the scrapped badge era (ADR-0018) had never tried the one vocabulary
the game itself teaches: the ability names.

A throwaway prototype (`build\hud-text-prototype.html`, six variants) put the
whole roster, venue and clock side by side and was pictured before any code
moved. Words won the eye test on every row. The window riders turned out to be
unambiguous without words — one window per lane, so a bare dotted countdown
after a lane's number cannot read as anything but that lane's window — with one
exception: the Roulette outcome, where which band spun is the information.

## Decision

The stack speaks words, and only words:

1. **The look is a text stack at the left-centre**: every chip draws its name —
   `Rush`, `Rend`, `Rotate`, `Aequilibrium`, `Roulette`, `All In`, `Gladius`,
   `Scutum`, `Momentum`, `Streak` — in Title Case, never keys, then its number,
   then its riders as `·`-separated dotted numbers. The block is anchored 4 gui
   from the left edge, vertically centred, rows 10 gui tall with a 2 gui gap (12
   pitch). No icons, no fills, no frames: shadowed text only.
2. **A single space holds the word and its number together**: rows read as written —
   `Roulette 13s`, `All In 34s`, `Streak 2` — so no label column and no padding. Column
   alignment was tried first and read as tab stops; words lead, numbers follow.
3. **Riders are bare dotted numbers**: `Rush 25s · 5s`. The loaded Rush bonus
   draws no marker — ADR-0020's `+` is dead — because the window countdown is the
   fact and the marker split one number into two pieces of information. The
   Roulette outcome rider keeps its word (`· Jackpot 8s`): one of six bands spun,
   and the word is the only thing that says which.
4. **The clock is unchanged** (ADR-0020 point 2): whole seconds, rounded up, a
   ready lane dim with no number. Dim now means a dim name; nothing else dims.
5. **No art ships**: the ten generated PNGs, `tools/generate-hud-icons.ps1` and
   `textures/gui/ability/` are deleted root and branch. A future icon pass is its
   own decision with its own prototype, not a leftover asset tree.
6. **The Momentum grace gets honest**: the snapshot carries the grace clock's
   stamp and `movedSince` compares it, so a hit at the stack cap — which
   re-stamps the clock without moving the count — still reaches the client. The
   rows decision prints the grace number only while live, so a stale snapshot can
   never draw a `0s`.
7. **Everything else holds**: the transport, the change points, the receipt-time
   interpolation, the hide rules, the zero-knob stance and the pure rows decision
   all carry from ADR-0020. The window nouns reach the code: no identifier,
   comment, test name or picker line says `stride` or `empower` — the stat is
   `SPEED_BONUS` at `infusedclasses:rush_speed`, the Rend bonus math is
   `RendAbility.bonusFor`, the runtime reads are `peekRendBonusStacks` and
   `consumeRendBonus`, and the Rush card reads `quickens you for 5s`.

## Consequences

- Nothing to maintain art-wise: retuning the stack is editing words and gaps in
  `StatusHud` and the element; no asset can drift from the roster.
- The stack needs no legend: fire lines, `/class info` and the stack all say the
  same names, and a remapped key cannot make the text lie.
- Numbers are ragged, one space after their own word — not column-aligned: the
  first cut padded every number to a common column and read as tab stops.
- `stride` and `empower` leave the vocabulary root and branch: identifiers,
  comments, tests, the picker copy and the prototype's data all speak the Rush
  window and the Rend window; only the past ADRs keep the old nouns, as history.
- The rider badge vocabulary retires: `IMP`, `TES`, `AEQ`, `ALL` and the outcome
  initials appear nowhere, and the `+` marker leaves the vocabulary with them.
- ADR-0020 is superseded on its look, badge and art points; its transport, clock
  and hiding decisions live on as cited.
- The prototype lands on the `prototype/hud-text-words` branch (which carries its
  own copy of the deleted icons, so the baseline variant still renders) and is
  never part of the build.
