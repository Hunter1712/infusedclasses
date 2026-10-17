# ADR-0017: The strip reads as pills and the action bar stops echoing the clock

Status: superseded by ADR-0018 (its silence-on-cooldown and compressed numbers carry on; the pill look does not)

## Context

The 0.3.0 strip drew each chip as a bare outline with the badge inside and a
readable side label trailing on the inner side — `[ULT] All In 70.7s [ALL]` —
so mirrored layout, side text and the vanilla action bar repeating the same
countdown (`All In: 10.7s`) collided into a wall of words. It played like debug
output: the numbers were outnumbered by names said twice.

## Decision

1. **Chips are self-contained pills**: flat translucent backdrop plus a
   one-pixel border, height 20, width fit to content; badge left, number
   right, ready lanes centered and dim, badgeless riders (stride, empower) a
   number-only pill. Still no icons, no textures, no animations — ADR-0014's
   art stance holds; its inner-side labels go.
2. **The strip hangs from the hotbar, not the screen corner**: rows pack
   inward from the hotbar's border on the chosen side (`hud.corner` keeps
   `bottom_right`/`bottom_left`, now naming the hotbar's side), the lowest
   row leaves a breath above the xp bar, the cluster keeps a hair off the
   border's pixel column, and dim keeps a legible silhouette at night —
   the first screenshot's corner-hung strip read as a clipped floating box.
   Everything is measured in scaled gui units beside vanilla's own hotbar
   geometry, so every gui scale lands the same frame.
3. **Words never enter the strip**: `Chip` loses `label` — ability, passive,
   outcome and stance names carry no pixels. The badge vocabulary
   (`SIG`/`ULT`, `MOM`, `STR`, `GLA`/`SCU`, `IMP`/`TES`, `AEQ`, `ALL`, outcome
   initials) plus numbers is the whole on-screen language; names live in fire
   lines and `/class info`.
4. **The strip owns the clock**: a press on cooldown says nothing on the
   action bar — `AbilityOutcome.onCooldown` loses its line, joining the
   wrong-class reject in silence, because the lane chip already counts that
   exact number live. Fire lines, fizzles and `/class info` keep speaking;
   `ChatLine.cooldown` survives as the `/class info` lane shape only.
5. **Numbers compress**: the strip shows whole seconds at ten and above
   (`25s`) and one decimal below (`4.8s`), the band starting at 9_950 ms so
   the tenths branch never rounds up to a flashing `10.0s`. Chat keeps
   `%.1fs` everywhere (ADR-0014 point 5 narrows from "everywhere" to "chat").
6. Config, hiding rules and the snapshot protocol are unchanged.

## Consequences

- Players who disable the HUD lose the countdown readout by design; the same
  lane table is one `/class info` away, and the fire line still says the
  ability happened.
- A rejected press is now fully silent server-side: the decision to press is
  made looking at the strip's lit countdown, not chat.
- The uniform box width ADR-0014 sized for the three-character badges is gone
  (width fits each chip), and the HUD element drops the side-label half of
  its drawing.
- An icon pass (ADR-0014's "later") slots into the pill's badge half without
  reopening this decision.
