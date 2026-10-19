# Spec: Cast fanfare — the evoker's cast spell

Status: done

## Goal

Follow-up from the card check (Oct 19 2026): the one shared cast fanfare
(`block.note_block.pling`) read underwhelming. Every fired ability now plays the evoker's own
cast spell instead — still one learnable fanfare, now one that sounds like a cast. The first
pass was inaudible on small speakers (the whoosh is sub-500 Hz), so it now plays at 1.6 volume.

## Decisions

- `AbilityApplier.CAST_SOUND` becomes `minecraft:entity.evoker.cast_spell`, played at 1.6
  volume (the shared play path stays 1.0 for crit cues); the fired-only rule is unchanged.
  Rotate still announces without a fanfare, and fizzles and rejects stay silent.
- One event, no layering and no fallback: the pling is not kept.

## Listen check seen

Oct 19 2026: the evoker cast at 1.6 volume reads over combat — Gambler's Roulette and All In,
the abilities that read silent before the bump — and rejects and fizzles stay silent. One shared
path, so this covers every class.
