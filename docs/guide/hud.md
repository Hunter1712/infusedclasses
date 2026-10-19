# Status HUD (the ability stack)

While you have a class, the mod draws a vertical stack of words on the left edge of your
screen, vertically centred. Each row is one ability or one piece of live state: the name
first, its countdown after it. Nothing else on screen shows class state, so this is the one
place to watch mid-fight.

## Reading the stack

- **A number is a countdown in whole seconds**, rounded up. `70s`, `9s`, `1s`. A countdown
  never reads `0`: the moment the time is up the number disappears.
- **A dim name with no number is ready.** Press it (`R` for your Signature, `V` for your
  Ultimate).
- **A bright name with a number is cooling.** The number is what is left.
- **One space between the name and its number**: the stack reads as written, so `Roulette 13s`
  sits over `All In 34s` and `Streak 2` with no padding — words lead, numbers follow, and
  the numbers are ragged down the stack.
- **Riders** are the dotted numbers after a lane's countdown, like `Offload 10s · 5s`. They
  track windows the abilities open — a bonus still loaded, a catch still open, a spin still
  running. A rider disappears the moment its window ends, so if it is not there, the bonus is
  not there. The Roulette outcome is the one rider that keeps its word (`· Jackpot 8s`):
  which band spun is the information.
- Text is white while live and grey for a ready lane; the stack never draws a backdrop, only
  shadowed text, so it reads over any terrain.

## Rows per class

| Row | Juggernaut | Legionary | Gambler |
|-----|------------|-----------|---------|
| Stance | — | `Gladius` / `Scutum`, no number | — |
| Signature | `Offload` | `Rotate` | `Roulette` |
| Ultimate | `Onslaught` | `Aequilibrium` | `All In` |
| State | `Momentum` (stacks · grace · checkpoint) | — | `Streak` (count) |

State rows appear only while they are above zero: a Juggernaut with no Momentum stacks has no
Momentum row, and a Gambler at Streak 0 has no Streak row. The Momentum row reads `12 · 5s`
while its grace clock is live, bare `12` the moment the clock has run out, and gains its
checkpoint word at 15: `22 · 5s · Braced`.

## Riders

- **Juggernaut**: the Offload window after its lane (`Offload 10s · 5s`) — the countdown is the
  whole story, since the price was paid at the press; the Onslaught window after its lane while
  it is live (`Onslaught 60s · 15s`), counting down to whichever of its window or its bar ends
  first.
- **Legionary**: the Rotate rider after a Gladius flip while the next-hit bonus is loaded, or
  while the Scutum catch is open; the Aequilibrium rider while its window runs.
- **Gambler**: the spin's outcome word after Roulette — `Edge`, `Ward`, `Haste`, `Jackpot` or
  `Bust`. Zero draws no rider: the dead band reads dead. The All In rider while its window
  runs.

The names are the same ones the fire lines and `/class info` use, never keys, so a remapped
key cannot make the stack lie. `/class info` keeps printing the same lanes with a one-decimal
countdown if you want the finer clock.

## Hiding

The stack is hidden while any screen is open, when the HUD is hidden (`F1`), and for players
with no class. There is no config file and no toggle: the stack sits on the left edge and
that is the whole layout.

## Look

The stack is text only — no icons and no textures ship with it. Each row is 10 gui tall with
a 2 gui gap, the anchor sits 4 gui from the left edge, and each chip draws its word, one
space and its number — no column padding. Retuning the look is editing `StatusHud`'s words
and the element's constants.
