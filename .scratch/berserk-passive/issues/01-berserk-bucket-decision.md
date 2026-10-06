# 01: Berserk bucket decision

**What to build:** Pure HP-to-bucket decision plus per-attribute modifier mapping for the Berserk pair. No player, level, or attribute instance touches.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] `bucket(missingFraction)` returns 0-10: `floor(missing * 10)`, clamped; full HP = 0, 0 HP = 10.
- [x] Damage maps to `bucket * 0.4` (`minecraft:attack_damage`, `infusedclasses:berserk_damage`, max +4.0).
- [x] Speed maps to `bucket * 0.08` (`minecraft:attack_speed`, `infusedclasses:berserk_speed`, max +0.8).
- [x] Bucket 0 yields absent modifier (remove), never a zero-valued write.
- [x] JUnit covers buckets at full / 90% / 50% / 5% / 0 HP plus clamp bounds, and bonus math at 0/5/10 buckets.

## Comments

Shipped as `HpScaledPassive.bucket(currentHp, maxHp)` (spec-solution shape: missing derived
inside, one place) plus `PassiveEffect.forBucket`. One deliberate deviation: exact HP
boundaries need a 1e-9 epsilon above the floor — float division lands a hair under whole
steps (90% HP reads 0.9999… without it). Covered in `BerserkTest`.
Follow-up from review: `forBucket` also clamps above-cap buckets to the cap, so the
mapping holds "Damage caps at +4.0" even for a misbehaving caller.
