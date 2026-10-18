# ADR-0013: Deepen the review's remaining seams

Status: accepted (point 2's cooldown remainder and its `onCooldown` factory are superseded by ADR-0024)

## Context

The architecture review (seven accepted candidates, 2026-10-16) found the living
seams still carrying two sources of truth each: the registry kept a parallel
ability-id list beside the `actives` rows it gated, six abilities each grew their
own `Kind`/`Result` protocol for the same fired / rejected / fizzled / on-cooldown
answers, the picker receiver and the command each sequenced validate → clean →
announce by hand, every ability window re-stated its duration at each read, the
melee settle sequence lived in `PassiveApplier.settleHit` while the pure plan
peeked beside it, the card's vertical geometry was spelled as chained pixel offsets
in the renderer, and the name/description tone boundary was an `indexOf` scan with
a fallback.

## Decision

1. **Actives carry their ability ids.** The parallel `activeAbilities` id list is
   deleted; `actives` holds `ActiveAbility(id, name, desc)` rows, `hasAbility`
   scans them, and flip buffs are their own `FlipBuff` row type.
2. **One ability outcome envelope.** `AbilityOutcome(fired, announcement,
   remainingMs)` with `fired` / `rejected` / `fizzled` / `onCooldown` factories;
   the six per-ability `Kind`/`Result` protocols are deleted, and abilities with a
   payload wrap the envelope (Rotate the stance, Roulette the band, Rend the
   consume flag).
3. **One class-switch path.** `ClassSwitch.trySwitch` owns validate → clean →
   announce for both entries and returns the new definition or null; the picker
   receiver keeps only the confirm packet, the command only its return code.
4. **Windows carry their duration.** `arm(now, durationMs[, payload])` stamps the
   duration beside the press timestamp, reads take only `now`, each ability names
   its window constant once at its arm site, and the flip window arms with the
   landing stance's constant.
5. **The melee plan resolves.** `MeleePlan.resolve` plans through `planHit` and
   settles the runtime movements in the same field order at the same `now`,
   returning `Landed(rendHp, verdict, combined)` for the entry to land exactly.
   `planHit` stays the package-private pure half; `PassiveApplier` keeps only the
   spender consume. This reopens ADR-0007's no-JUnit cost for the settle seam:
   the ordering is asserted through `resolve` without a player.
6. **Card geometry lives in CardLayout.** Name/tagline/divider anchors, the derived
   body top, the divider height and the inner width come from `CardLayout`; the
   renderer draws at anchors and measures the whole body once per frame
   (`measure` → `Measured(rows, layout)`); the screen clamps this frame's scroll
   from that measurement before drawing, and row clipping uses `font.lineHeight`.
7. **The tone boundary derives from the join.** `CardLine.descStart()` computes
   where the description starts in `text()`; the wrapper feeds it to rows, and the
   `indexOf` scan plus its fallback are deleted.

## Consequences

- Behaviour is unchanged throughout; every candidate is a seam move, not a rules
  change.
- The registry cannot drift a gated id from its card row; `hasAbility` reads the
  one list.
- Call sites ask one `fired()` question; a wrong class and a cooling lane return
  the same silent reject — the cooldown remainder folded away with ADR-0024, and
  tests pin the lane's clock from the runtime instead.
- Switch entries cannot sequence differently; callers keep only their own extra.
- Window reads cannot re-state a duration, and a flip's window follows its landing
  stance.
- The settle ordering is JUnit-reachable through `resolve`; `passiveForTrigger`
  still keys the Rush credit and the Rend snapshot on what the class carries.
