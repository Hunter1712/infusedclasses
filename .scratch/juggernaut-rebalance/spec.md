# Spec: Juggernaut rebalance (grill rounds)

Status: done

## Decisions (user-set)

- Momentum: 20 stacks, +0.4 armor each (max +8), 5s grace kept.
- Vigor damage: floor -1.0 at 0 HP, +0.3 per step, +2.0 full. Speed: floor -0.25,
  +0.075 per step, +0.5 full (same -50%-of-max shape).
- Rush: no Speed mob effect; +0.02 movement-speed stride stat while the 5s
  window is live (tick-written, write-only-on-change); +5 stacks; 25s lane kept.
- Rend: zero base, +0.5 per stack, 10 HP at the full 20; 60s lane and 6s window kept.

## Notes

- Dummy targets (mod training dummies) skip the living-damage hook: unsupported, accepted.
- Negative Vigor floors ride the attribute; vanilla clamps at zero where it must.
- Copy regenerates from constants; active-desc 200-char cap still holds.
- Dead zero deleted with the rebalance: `ClassDefinition.maxHealthBonus` plus the `MAX_HEALTH`/`BONUS_HEALTH_ID` registry keys (always 0.0, vanilla 20 HP stands).
