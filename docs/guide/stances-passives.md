# Stances & passives

How the mod's persistent bonuses work: what earns them, what spends them, and what survives a relog. Numbers here quote the in-game picker copy, which is generated from the same constants as behaviour so the two cannot drift.

## Conditional passives (Juggernaut)

**Momentum** — `on_hit_dealt` and `on_hit_taken`, +1 each, max 60, 5s grace, then -1/s.
Picker line: `Land a hit: +1. Take a hit: +1. Max 60. 5s no feed: -1/s.` followed by the three
checkpoints:

- 15 **Steady**: +1 armor, -2% speed, +0.25 knockback resistance.
- 30 **Braced**: +3 armor, -4% speed, +0.5 knockback resistance.
- 45 **Rooted**: +5 armor, -7% speed, +1 knockback resistance. Past 45 stays Rooted; 45-60 is
  the actives' spend reserve.

The bar reads its last reached checkpoint every tick, so losing a tier takes the modifiers off
with it.

- Dealt: only a full-swing landing hit earns. Taken: any landed damaging hit earns. A hit
  fully eaten by a raised shield earns nothing on either side.
- Offload pays a flat 5. Onslaught needs 10 and spends 2 per full hit inside its window.
- Onslaught stops the feed while live: dealt and taken hits add nothing.
- Transient: counts, last-feed stamps and the next-loss clock live in the session runtime,
  keyed by passive definition. Relog, respawn and class switch all clear them. A class switch
  removes what the old class earned but the new one does not carry.

## Always-on chassis (Juggernaut)

**Burden** — +1 attack damage (`infusedclasses:burden_damage`), -0.5 attack speed
(`infusedclasses:burden_swing`). Written every tick while classed, never earned.

## HP-scaled machinery

No class registers an HP-scaled passive today. The shared bucket machinery remains for a later
kit; Vigor retired with the Momentum rework.

## Stances (Legionary)

Not stacks: transient auras applied and removed at once on flip. Only the current stance's modifiers sit on the player; leaving the class takes them all off.

| | Damage | Speed | Armor | Toughness | Move | Deal mult | Take mult |
|---|---|---|---|---|---|---|---|
| Gladius | +1.0 | +0.3 | — | — | — | — | x1.1 taken |
| Scutum (default) | — | — | +3 | +1 | about -6% | x0.9 dealt | — |

**Flip buffs** ride the flip record, not a lane:

- **Impetus**: flip to Gladius → next melee hit x1.10, any charge, one-shot, 5s expiry. Flipping away drops it.
- **Testudo**: flip to Scutum → melee taken x0.85 for 1.5s. Timed, never consumed.

**Aequilibrium** window: +1.0 damage +3.0 armor for 6s on the 50s oath lane, either stance, both layers count.

## Cooldowns (transient lanes)

| Lane | Length | Verbs |
|---|---|---|
| Offload | 10s | fire only when ready, fizzle arms nothing |
| Onslaught | 60s | fire only when ready, fizzle arms nothing |
| Rotate | 8s | the only gate on both switch buffs |
| Oath (Aequilibrium) | 50s | owned solely by the oath |

`reset` readies every lane again for class switch and respawn. Offload's window, Testudo,
Aequilibrium and All In are timed (peeked while live, never consumed); Onslaught's window ends
with its 15s or with the bar, whichever empties first; Impetus is the one-shot (peeked, then
spent on the next landed hit).
