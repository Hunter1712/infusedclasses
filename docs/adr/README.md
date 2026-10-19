# ADR index

One line per decision: what it settles, and whether a later ADR revised it. Read
the entries that touch the area you are about to change — `/code-review` and
`/improve-codebase-architecture` both start from this list. Unmarked entries are
accepted; each file is the full record.

Add the line in the same change that creates the ADR, and update the marker when
a later decision revises or supersedes a point.

- [0001 — Decide purely, apply in an adapter](0001-decide-purely-apply-in-an-adapter.md) — decisions take data and return a description of an outcome; adapters apply it.
- [0002 — Transient per-player state is owned by the server entry](0002-transient-state-lifetime.md) — the class choice module stays live-free; the entry's runtime owns the transient half.
- [0003 — Class lookup stays a direct registry call until a second source of classes exists](0003-defer-class-lookup-seam.md) — no lookup interface; a deliberate deferral, reversed by its own trigger.
- [0004 — Juggernaut inversion (high-HP rewards) and root-to-sound swap](0004-juggernaut-inversion.md) — Fury→Vigor (current HP), Bulwark→Momentum (hit-dealt); heals help instead of cooling. *(superseded by 0027)*
- [0005 — Hit gating, stack storage, and trigger-scan seams](0005-hit-gating-stack-seams.md) — the entry never compares ability ids to gate rewards; gating stays in the pure modules.
- [0006 — Legionary stances plus the universal charged curve and stack gate](0006-legionary-stances-quad-curve.md) — bonus damage is `bonus x charge x charge`; base weapon damage is never touched. *(the curve point revised by 0028; the stack gate stands)*
- [0007 — Melee plan, damage settle, bind morph, and adapter splits](0007-melee-plan-settle-morph-splits.md) — `MeleePlan` decides a hit's earn and settle, `DamageSettle` applies; reopens 0005's bind-morph carve-out.
- [0008 — Close the six architecture-review seams](0008-review-seam-closures.md) — the second review's six seams close: cooldown lanes stay the runtime's implementation, Rend lands through the shared announce path. *(its Rush adapter split superseded by 0027)*
- [0009 — Oath dispatch stays in the server entry](0009-oath-dispatch-stays-in-entry.md) — the three-way switch stays explicit in the wiring; `BindMorph` decides what a reused bind means and `SlotPresses` maps it. *(point 3 superseded by 0026; point 1 superseded by 0029 — the row's own fire is pure, so the switch retires)*
- [0010 — Pay the flip, not the camp (switch buffs + Aequilibrium)](0010-switch-buffs-aequilibrium.md) — stances nerfed at half tax; `Impetus`/`Testudo` move to flip buffs off one flip record.
- [0011 — Fold the stance adapter and the flip holders home](0011-fold-stance-and-flip-holders.md) — stance application becomes private to `PassiveApplier`; reopens 0007.3 for the stance case only.
- [0012 — The mod owns crits and chance enters decisions as data](0012-mod-owns-crits.md) — vanilla crits retired for players; chance arrives as data.
- [0013 — Deepen the review's remaining seams](0013-deepen-review-seams.md) — actives carry their ability ids; one ability-outcome envelope. *(point 2 superseded by 0024; its fire return shape revised by 0029)*
- [0014 — The status HUD returns as the slot strip](0014-status-hud-slot-strip.md) — one vertical strip of slot-style chips. *(superseded by 0017, then 0019)*
- [0015 — Re-cut the wheel with no healing](0015-wheel-recut-no-heal.md) — the heal path is deleted, not disabled; six bands, odds re-cut.
- [0016 — The ultimate's own bind — Signature R, Ultimate V](0016-ultimate-bind-slots.md) — two binds by slot; the morph keeps its matrix, changes its vocabulary. *(point 2's target vocabulary superseded by 0029)*
- [0017 — The strip reads as pills and the action bar stops echoing the clock](0017-strip-pills-own-the-clock.md) — self-contained pills drawn from the roster. *(superseded by 0018; its silence-on-cooldown and compressed numbers carry on)*
- [0018 — The strip goes bare — a stack in a screen zone](0018-strip-goes-bare.md) — bare text rows, no fill, no outline, no sprite. *(superseded by 0019)*
- [0019 — The strip leaves the build](0019-strip-leaves-the-build.md) — the status HUD chain is scrapped pending a later implementation. *(superseded by 0020, then 0021)*
- [0020 — The ability HUD returns as the left-centre icon stack](0020-ability-hud-returns.md) — an icon row per chip family, anchored left-centre. *(superseded by 0021)*
- [0021 — The stack speaks words](0021-the-stack-speaks-words.md) — every chip draws its name in Title Case, never keys, then its number. *(point 7's change-point carry superseded by 0022)*
- [0022 — One steady-state emission point](0022-one-steady-state-emission-point.md) — the server tick's rebuild-and-compare carries every emission; post-branch sends die.
- [0023 — The roster row carries the stack's view](0023-roster-carries-the-stack-view.md) — a row carries its lane read plus rider rules. *(point 1's `View` and point 2's ultimate-bind sentence revised by 0025 and 0026)*
- [0024 — One silent reject](0024-one-silent-reject.md) — a rejected press says nothing, grants nothing, arms nothing (`AbilityOutcome.rejected()`).
- [0025 — The lane is the key](0025-the-lane-is-the-key.md) — `Lane` owns the cooldown lengths; cooldowns, windows, payload and roster share one lane-keyed vocabulary.
- [0026 — The ultimate is not lent](0026-the-ultimate-is-not-lent.md) — each target names the ability it fires; one walk of the class's own actives. *(its printed fallback names renamed by 0027; its target names replaced by row slots in 0029)*
- [0027 — Momentum is the Juggernaut's one mechanic](0027-momentum-the-one-mechanic.md) — a 60-stack bar with 15/30/45 checkpoints, spent by Offload and Onslaught; Burden is the chassis; Vigor, Rush, Rend and the charged curve retire. *(its Onslaught pay shape, Burden damage row and movement weights revised by 0028)*
- [0028 — One unit per stat family](0028-one-unit-per-stat-family.md) — damage power is a deal mult (the four `+1` flats become x1.15 composed in the plan's combined); attribute stats stay flat in their own unit; the curve retires and `FullSwing.isFull` keeps the gate; balance gets a ±10% target and a sheet.
- [0029 — The ability row owns its slot and its fire](0029-ability-row-owns-slot-and-fire.md) — `Row` carries `Slot`, fanfare and `Fire`; `BindMorph` is a slot walk; `SlotPresses` is one press; every `fire` returns `AbilityOutcome`. *(supersedes 0009 point 1; revises 0013 point 2's fire shape and 0026's target names)*
- [0030 — Each decision lives beside what it applies to](0030-decisions-live-beside-what-they-apply.md) — `WindowFamily` rows sit on their abilities; `ConditionalPassive.barRows` puts each bar's rows on the bar; the applier is one loop.
- [0031 — The status snapshot composes the runtime reads](0031-snapshot-composes-runtime-reads.md) — the payload is class id + `PlayerRuntime.State` + `PlayerRuntime.BarRead`; `State.movedSince` owns the armed comparison; `TickPass.run` names the tick sequence.
