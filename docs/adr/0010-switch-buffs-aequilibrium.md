# ADR-0010: Pay the flip, not the camp (switch buffs + Aequilibrium)

Status: accepted

## Context

Legionary identity sat in always-on stance auras (Gladius +2.0 damage / take x1.2,
Scutum +6 armor / deal x0.8) plus the stance-morphed oath pair on `G` (Impetus +3
through the curve / take x1.25, Testudo +8 armor and brace / deal x0.7, 6s windows
on a shared 50s lane). Camping a stance paid full value with no further decision
after the `Rotate`. Agreed direction: halve the always-on stats, pay for the flip
itself under the existing names, and give `G` one clean ultimate.

## Decision

1. Base stances stay, nerfed: Gladius +1.0 damage / take x1.1 / +0.3 speed, Scutum
   +3 armor / deal x0.9 / -0.006 weight, toughness unchanged. Glass still reads as
   glass, plate as plate, at half tax.
2. `Impetus`/`Testudo` move from oath windows to flip buffs off one flip record
   (`flipAt` + `flippedTo`, stamped by `flipStance(now, landing)`): Gladius-flip
   arms a one-shot next-hit x1.10 at any charge (5s, peek/consume mirroring Rush),
   Scutum-flip opens a timed take-x0.85 catch (1.5s, never consumed). The 8s
   `Rotate` lock is now the only gate on both.
3. The 6s-window + shared-lane machinery moves to the new `G` ultimate
   `Aequilibrium` (+1.0 damage, +3.0 armor, no downsides, either stance), reusing
   `Cooldowns.OATH_MS` and the `useOath`/`oathRemaining` verbs unchanged. `G` never
   meant Impetus/Testudo again, so the stance morph is deleted: `BindMorph`
   answers `AEQUILIBRIUM` for every Legionary.
4. The entry keeps the two-way switch (ADR-0009's shape, one target fewer), and
   the stance applier trades the Testudo plate/brace branch for the Aequilibrium
   aura. Copy stays generated from constants; the Impetus/Testudo announcements
   go with the oath fire path they belonged to, since nothing fires them anymore.

## Consequences

- Combined peaks are intentional and gated: switch x1.10 multiplies the +1 edge,
  x0.85 multiplies under the +3 plate, held by the 8s lock plus the 50s lane.
- Cost: the flip record is one more transient fact beside the windows, and the
  Impetus one-shot adds a pending flag next to the timed Testudo peek. Both die
  with the session like everything else in the runtime.
