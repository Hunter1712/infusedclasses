# Stances & passives

How the mod's persistent bonuses work: what earns them, what spends them, and what survives a relog. Numbers here quote the in-game picker copy, which is generated from the same constants as behaviour so the two cannot drift.

## Conditional passives (Juggernaut)

**Momentum** — `on_hit_dealt`, max 20 stacks, `minecraft:armor` +0.4 per stack (max +8.0), 5s grace, all stacks drop at once after. Picker line: `Land a hit: +0.4 armor (max 20). 5s no hit: lose all.`

- Earned only by full-swing landing hits (stack gate over the bonus curve). Spam earns zero.
- Rush's one-shot bonus adds +5 on the next full-swing hit inside its 5s window.
- Rend spends every stack plus the armor up front. Zero stacks fizzles.
- Transient: counts plus last-trigger timestamps live in the session runtime, keyed by passive definition. Relog, respawn and class switch all clear them. A class switch removes what the old class earned but the new one does not carry.

## HP-scaled pair (Juggernaut)

**Vigor** — current HP maps to stepped buckets, 10% per step over 10 steps, each step adding a fixed bonus on top of a floor. Reads as one line with both bonus descs joined, so one passive never looks like two.

- Damage: +0.3 attack damage per step from -1.0 (-1 at 0 HP, +2.0 full).
- Speed: +0.075 attack speed per step from -0.25 (-0.25 at 0 HP, +0.5 full).
- Recomputed every tick from live HP, transient and never saved. Rend never touches it.

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
| Rush | 25s | fire only when ready, rejects leave everything untouched |
| Rend | 60s | fire only when ready, fizzle arms nothing |
| Rotate | 8s | the only gate on both switch buffs |
| Oath (Aequilibrium) | 50s | owned solely by the oath |

`reset` readies every lane again for class switch and respawn. The Rush/Rend engage windows are one-shot (peeked, then spent on the next hit, each bonus landing at most once); Testudo and Aequilibrium windows are timed (peeked while live, never consumed).
