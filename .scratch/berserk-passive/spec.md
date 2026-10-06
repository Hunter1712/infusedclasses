# Spec: Berserk passive (low-HP damage + speed)

Status: done

## Problem Statement

Warrior rewards staying in the fight with Bulwark armor (`on_hit_taken`, +1 armor/stack, max 10) and spends it via Second Wind, but has no comeback tool: a Warrior at low HP fights exactly as hard as one at full HP. Player wants passives where damage and attack speed rise as HP falls, so near-death feels dangerous instead of hopeless.

## Solution

Add a second Warrior passive pair alongside Bulwark, both continuous HP-scaled (not `ConditionalPassive` stacks): missing HP maps to stepped buckets, each bucket adds a fixed bonus. Two mirrored single-attribute definitions — one `minecraft:attack_damage`, one `minecraft:attack_speed` — sharing one curve: 10% missing HP = 1 step, 10 steps total, zero bonus at full HP, max at 0 HP. Damage caps at +4.0 total (+0.4/step); attack speed caps at +0.8 total (+20% on 4.0 base, +0.08/step). Recomputed every server tick from current HP, written only on bucket change. Transient only; heals shrink it automatically; Second Wind consume touches Bulwark only.

## User Stories

1. As a Warrior at half HP, I want noticeably harder hits and faster swings, so that low HP feels like a threat to my enemy.
2. As a Warrior at full HP, I want no bonus, so that the passive never reads as free stats.
3. As a Warrior healed by Second Wind, I want my bonus to shrink as my HP refills, so that stats stay honest.
4. As a Warrior browsing the picker, I want to see the Berserk rule text with its caps, so that I know what it does before choosing.
5. As a Warrior who dies or relogs, I want no stale bonus surviving, so that respawn starts clean.

## Implementation Decisions

- Decide-then-apply: new pure HP-scaled decision (given max HP, current HP, return bucket 0-10) plus `PassiveEffect.Modifier` mapping per attribute; `PassiveApplier` owns all attribute writes. Decision never touches player, level, or attribute instance (ADR-0001).
- Not `ConditionalPassive`: no trigger, no stacks, no grace, no `tickDecayMoved`. Bucket math is `floor(missingFraction * 10)`, clamped 0-10. Bonus is `bucket * perStep` per attribute.
- Split: two single-attribute definitions, same curve — `minecraft:attack_damage` under `infusedclasses:berserk_damage` (+0.4/step, max +4.0), `minecraft:attack_speed` under `infusedclasses:berserk_speed` (+0.08/step, max +0.8). Keeps the one-attribute-per-decision shape; cost is one extra loop iteration per tick, writes only on change.
- Refresh: every server tick in the existing passive tick path, from live HP; `write()` early-out when `existing.amount() == wanted.amount()` already covers no-op ticks. Zero bonus (bucket 0) means remove, same as zero-stacks plan.
- Lifecycle: transient, never saved to NBT. `onClassChanged` removes both modifiers and clears nothing else (no stacks to clear); respawn/logout removes them via the existing transient clear path. `consumeSpent` / Second Wind resolves Bulwark by trigger and never touches Berserk.
- Picker copy generated from the numbers (`Each 10% missing HP grants +0.4 damage / +0.08 speed, max +4.0 / +0.8 at 0 HP`-style), card body shows passive names only, tooltip shows full form — same `CardLine` / tooltip path as Bulwark.
- Warrior `ClassDefinition` gains the two passives; Bulwark definition, grace, and Second Wind math unchanged.

## Testing Decisions

- Pure JUnit on bucket math: full HP = 0, 90% HP = 1, 50% HP = 5, 5% HP = 9, 0 HP = 10; clamp above max / below zero; bonus = bucket * perStep per attribute at 0/5/10 buckets.
- Plan/apply: bucket 0 after movement removes the modifier; same bucket twice writes once; heal shrinking bucket removes/writes down; Second Wind consume leaves Berserk modifiers in place.
- Presentation: card lines list both passives, tooltip copy matches numbers exactly.
- Not tested: tick plumbing, HUD, balance feel — manual dev-client verification.

## Out of Scope

- Threshold/all-or-nothing variant (ships stepped-linear only).
- Persisting bonus across relog/respawn.
- New HUD row or low-HP screen effects.
- Touching Bulwark numbers, grace, or Second Wind heal/cooldown.
- Balance tuning beyond +4.0 / +0.8 / 10% steps.
