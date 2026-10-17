# Status HUD

The strip of badge chips showing your class's live state while you play — lanes, windows, stacks and stance — so nothing needs counting in your head. It draws what the server last told it about your class and ticks the countdowns itself; it never changes how anything plays, and it never spreads beyond the two config knobs below.

## Where it sits

One vertical strip anchored to a screen corner — bottom right by default, since chat owns the left. It grows up from the corner with its labels on the inner side, mirrored per corner: at `bottom_right` the text reads to the left of the chips, at `bottom_left` it reads to the right. Both corners clear chat and the hotbar.

The badge boxes are vanilla-slot style: a uniform box, one height (20), letters and numbers only — no icons, no textures, no animations. A lit chip is what is counting now; a dim chip with no number is a lane ready to fire.

Move it or turn it off in the client config, [Configuration](configuration.md): `hud.enabled` stops the drawing and nothing else; `hud.corner` is `bottom_right` or `bottom_left`. The file is read once at startup, so edit and restart the game to apply.

## The roster

Badges name slots and states, never keys: **SIG** and **ULT** pack the signature and ultimate *slots* and their lane cooldowns, the rest name abilities, states or windows. Where the real keys are is [Controls], whatever they are on your machine, so the strip stays honest under remaps.

| Badge | Class | What it counts | When it appears |
|---|---|---|---|
| `SIG` | all | the signature lane: Rush 25s / Rotate 8s / Roulette 25s | always; dim with no number while ready, lit with a countdown while cooling |
| `ULT` | all | the ultimate lane: Rend 60s / the 50s oath / All In 75s | always; dim while ready, lit with a countdown while cooling |
| `GLA` / `SCU` | Legionary | your current stance, Gladius or Scutum | always; drops off the strip the moment you flip |
| `IMP` | Legionary | the Impetus window, 5s from your flip to Gladius | while the next-hit bonus is unspent; the first landed hit removes it, expiry does too |
| `TES` | Legionary | the Testudo window, 1.5s from your flip to Scutum | while the timed catch is live; expiry removes it |
| `AEQ` | Legionary | the Aequilibrium window, 6s | while the oath buff is live |
| `MOM` | Juggernaut | your Momentum stack count plus the 5s grace countdown, `2 4.0s`-style | while you hold at least one stack |
| stride rider | Juggernaut | the 5s Rush stride window | while the window lives; a `+` marks the next-hit bonus as unspent, and it drops to a plain countdown the moment a full swing spends it — the chip keeps counting to expiry |
| empower rider | Juggernaut | the 6s Rend empower window | while Rend's next-hit snapshot is unspent; the landed empowered hit removes it |
| `E` `W` `H` `J` `B` | Gambler | the Roulette outcome — Edge, Ward, Haste, Jackpot, Bust — plus its 10s countdown | from the spin until the window ends; each band's own initial stands for it |
| `ALL` | Gambler | the All In window, 15s | while the doubled crits are live |
| `STR` | Gambler | your Streak count | above zero; a crit resets to nothing again |

Two absences are deliberate: a **Zero** spin shows no chip at all (the dead band reads dead), and **Vigor** shows nothing on the strip — it is always-on, HP-derived, and there is no decision to make about it.

## Numbers

Every countdown is one decimal — `24.0s`, `1.5s`, `0.0s` — and clamps at zero rather than reading negative. The ability chat lines use the same `%.1f` precision, so the strip and the chat line agree on what half a second is.

## When it hides

- **F1**: vanilla's hide-HUD.
- **Any open screen**: picker, chat, inventory, pause — the strip pauses with the rest of the HUD.
- **No class, or no snapshot yet**: classless players draw nothing at all.
- **`hud.enabled=false`**: stops the drawing only; play is unchanged.

A class switch wipes the strip whole, and respawn and relog re-seed it from a fresh server snapshot — the strip is session data and carries nothing across a disconnect. What each class's abilities do is in [Stances & passives](stances-passives.md) and [Classes](classes.md); how the strip gets its state is one server snapshot at every change, never a per-tick feed.
