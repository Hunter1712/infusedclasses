# Spec: Audit fixes 02

Status: implemented — shipped in 0.5.0; in-game checks owed

## Problem Statement

The second full audit found three real bugs and six smaller gaps.

A player with the mod and a modified client can send the class-pick packet at any time. The server accepts it, changes the class, and resets every cooldown, stack and window. The class choice stops being a choice, and a player can switch to a counter-class mid-fight or refresh an ultimate on demand.

A killing blow skips the whole melee plan. Fabric's damage event does not fire when the hit kills the target, so the hit that finishes a fight grants no Momentum, pays no Onslaught price, spends no Impetus, moves no Streak and plays no crit cue. ADR-0012 only drops the extra damage on a kill, not the state changes.

The settle bonus is scaled by difficulty a second time. The settle type opts into `when_caused_by_living_non_player`, so a mob's hit on a Gladius Legionary scales the base hit once, then scales the x1.2 bonus again. On Hard, a 20-damage hit with no armor lands 30 plus a bonus of 9 instead of 6.

The smaller gaps: the client HUD anchors countdowns to the wall clock while the server uses monotonic time; the resistance and protection layers that price every bonus have no test (armor stays vanilla's, behind a live-entity call); `/class set` sends its feedback to the target instead of the command source; the picker footer tells players to click a card that is not a button; a low-stack press on a cooling lane says `No Momentum` even though the guide promises cooldown presses are silent; and the retained HP-scaled passive path has no test with a real definition.

## Solution

Make the server the only authority on when a class pick is allowed. Run the same state-only melee plan on a killing blow as on any other landed hit, while keeping the extra damage dropped on a kill. Make the settle bonus difficulty-independent. Move the client HUD's local anchor to a monotonic clock. Pin the mitigation arithmetic, the settle type and the retained HP-scaled path with tests. Fix the command feedback, the picker footer and the cooldown precedence.

No class numbers or balance knobs change; the fixes restore the behavior the cards and guide already promise. No new ability, class or layer. The work reuses the existing decide-then-apply seams and adds one small pure seam for the picker grant and one for the mitigation arithmetic.

## User Stories

1. As a server operator, I want a player to change class only when the server has opened the picker for them, so that a modified client cannot switch mid-fight.
2. As a server operator, I want a class-pick packet without a picker grant to be rejected before any class change, so that cooldowns and stacks cannot be reset at will.
3. As a server operator, I want the picker grant to be transient and per-player, so that a relog or another player cannot reuse it.
4. As an admin, I want `/class select` and `/class pick` to grant the picker, so that the GUI test path still works for a player who already has a class.
5. As an admin, I want `/class set` to change a class without a picker, so that I can fix a choice directly.
6. As an admin, I want a successful `/class set` to clear any pending picker grant, so that the player cannot immediately pick a second class.
7. As a player, I want a rejected pick to keep the picker open for retry, so that a typo does not close the screen.
8. As a player, I want my class choice to stay saved across relog, so that the new gate does not reset it.
9. As a Juggernaut, I want a killing blow to add Momentum, so that finishing a target still fills my bar.
10. As a Juggernaut in Onslaught, I want a killing blow to pay the 2-stack price, so that the ultimate is not free on the hit that kills.
11. As a Juggernaut in Onslaught, I want a killing blow that empties the bar to end the window, so that the ultimate lasts the bar as documented.
12. As a Legionary with Impetus pending, I want a killing blow to spend it, so that the buff does not carry into the next fight.
13. As a Gambler, I want a killing blow to move Streak, so that a finishing hit counts as a landed hit.
14. As a player, I want a killing crit to play the crit cue, so that the hit reads as a crit.
15. As a player, I want a killing blow to skip the extra settle damage on the dead target, so that death handling stays vanilla.
16. As a Legionary in Gladius, I want a mob's hit to land exactly x1.2 of the base after the settle, on every difficulty, so that the tradeoff is true.
17. As a server operator, I want the settle bonus to ignore difficulty scaling, so that only the base hit is scaled.
18. As a Legionary, I want the settle bonus to keep bypassing armor, effects and shields once, so that the scaling fix does not re-price mitigation.
19. As a client player, I want the HUD countdown to stay correct when my computer's clock changes, so that I can trust the cooldown display.
20. As a client player, I want the HUD to keep its local anchor, so that the fix does not add a server clock packet.
21. As a maintainer, I want the resistance and protection arithmetic covered by a plain JUnit test, so that a typo in those layers fails the suite instead of changing damage silently. Vanilla owns the armor formula.
22. As an admin, I want `/class set` to tell me whether it worked, so that I can distinguish a typo from a silent failure.
23. As a new player, I want the picker footer to name the Select button, so that I do not click a card that does nothing.
24. As a Juggernaut, I want a press on a cooling lane to stay silent even when I am below the price, so that the guide's rule is true.
25. As a player, I want a low-stack press off cooldown to say `No Momentum`, so that I know why the ability did not fire.
26. As a maintainer, I want the HP-scaled path covered by one test with a real definition, so that the retained machinery's cleanup and card paths are exercised.
27. As a future kit author, I want the HP-scaled cleanup and card grouping to be tested, so that adding a class with an HP-scaled passive does not ship broken wiring.
28. As a maintainer, I want the fixes to reuse the existing decide-then-apply seams, so that the change does not add a layer.
29. As a maintainer, I want exactly one new pure seam for the picker grant and one for the mitigation arithmetic, so that the test surface stays small.
30. As a maintainer, I want the round verified by the JUnit suite plus the AGENTS.md in-game hand-checks, so that event wiring and client copy are not declared done on compile alone.

## Implementation Decisions

- **Picker grant.** Add a small server-entry module, `PickerGrants`, holding a transient per-player set of UUIDs. Grant a player when the join picker is sent, and when `/class select` or `/class pick` opens the screen. The `select_class` receiver checks the grant before calling the class switch; a successful switch consumes it, a rejected pick keeps it so the picker can retry. Clear the grant on disconnect, respawn and a successful `/class set`. The grant is not NBT and not part of `PlayerRuntime`; `PlayerClasses` stays the pure, saved class-choice module. The receiver ignores a packet with no grant, so no new chat line is needed for the modified-client case.
- **Lethal-hit state.** Register an `AFTER_DEATH` branch for player melee kills. The branch carries the same `DamageSettle.isResolving()` guard as the hit hook, so a bonus settle that kills cannot run the plan twice. It checks the same attacker and direct-entity shape as the existing hit hook, reads the captured swing charge and draws the same full-swing roll the hit hook would have drawn, then calls the state-only melee plan with a positive dummy damage amount and no victim, because a kill always took damage. It cues the verdict but skips the settle, because the target is dead and ADR-0012 already drops the extra damage on a kill. The existing `AFTER_DAMAGE` path stays unchanged for non-lethal hits. No new melee decision module.
- **Settle scaling.** The `infusedclasses:settle` damage type uses `scaling: never`. It keeps its bypass tags for armor, effects and shields, and its zero exhaustion. No production code changes.
- **Client clock.** `StatusStore` and the HUD element use monotonic milliseconds for the receipt time and the elapsed read. The anchor stays client-local; no server clock packet and no change to the snapshot shape.
- **Mitigation arithmetic.** Extract the pure resistance and protection arithmetic from `HitMitigation.landedOn` into a helper that takes the damage after vanilla armor, the Resistance amplifier, the protection amount and the three bypass flags (`BYPASSES_EFFECTS`, `BYPASSES_RESISTANCE`, `BYPASSES_ENCHANTMENTS`). Armor stays vanilla's `CombatRules` because its armor-effectiveness enchantment needs the live entity and source. `landedOn` keeps reading the live entity and delegates. The helper is the new pure seam for this fix; the production call path is unchanged.
- **Command feedback.** `ClassCommands.setClass` sends a success or failure line only to the command source when the source is not the target; when they are the same, the existing class-choice line is enough. Add two `ChatLine` factories for the command source so the wording stays in the line module; the target keeps the existing class-choice lines. The command still returns 1 on success and 0 on rejection for Brigadier.
- **Picker footer.** Replace the footer literal with “Use Select to choose it”. Do not add a card click handler; the Select button is the one control.
- **Cooldown precedence.** In the Offload and Onslaught triggers, check the lane cooldown before the stack price. A cooling lane returns `rejected()` (silent); an off-cooldown low-stack press returns `fizzled("No Momentum")`. Update the Javadocs and the guide's Offload/Onslaught rows so the low-stack line is scoped to a ready lane. This preserves ADR-0017 and ADR-0024 and makes the guide's cooldown sentence true.
- **HP-scaled machinery.** Keep the path, because the guide says it is retained for a later kit. Add one test definition with a non-empty `hpPassives` list and assert the pure cleanup and card grouping paths. The live applier tick remains a future in-game check if a kit lands. Deletion is a separate product decision.
- **No new ADR.** The picker gate enforces the guide's existing “picker opens once” rule. The lethal-hit branch restores the existing melee-plan rule. The cooldown order preserves ADR-0017. The client clock is a bug fix. Add `Picker grant` to `GLOSSARY.md`; no other new domain term is introduced.

## Testing Decisions

A good test here asserts the player-visible rule, not the wiring. It constructs no `ServerPlayer`, level or screen, and it matches the repo's existing pure-JUnit style. The event registration, command output, picker footer and clock source are adapter changes, verified in game rather than through a new harness.

- **Picker grant:** a new pure `PickerGrantsTest` covers grant, clear, per-player isolation and the one-shot rule; the successful switch clears the grant through `ClassSwitch`. The receiver's gate and the join/command grant points are verified in game by sending a second pick without a grant and seeing the class and cooldowns unchanged. Prior art: `CooldownsTest` for the one-shot lane state.
- **Lethal hit:** the existing `MeleePlanTest` already pins the Onslaught price, Impetus spend and Streak moves for a landed full swing; the new part is the `AFTER_DEATH` adapter, verified in game with killing blows for Onslaught, Impetus, Streak and a crit cue. No duplicate pure test. Prior art: the existing `MeleePlanTest` hit-plan cases.
- **Settle scaling:** a small resource test reads the settle damage type and asserts `scaling: never`. The real check is in game on Hard and Easy with a mob hit on a Gladius Legionary. Prior art: `ProtocolTest` for data-shape pinning.
- **Client clock:** the existing `StatusStoreTest` and `StatusHudTest` already pin the elapsed math; the clock-source change is a two-line wiring change verified by reading and, optionally, an in-game clock change.
- **Mitigation:** a new `HitMitigationTest` calls the pure helper from an already-armored amount for no Resistance, Resistance I and II, protection, and each bypass tag, with values that distinguish each branch. The vanilla armor call and the live `landedOn` path remain an in-game check on a real hit. Prior art: `DamageSettleTest` and `MeleePlanTest`.
- **Command feedback:** no JUnit; the in-game check runs `/class set` on another player and sees a source line on both success and rejection. Prior art: the guide's command table.
- **Picker footer:** no JUnit; the in-game picker opens and shows “Use Select to choose it”. Prior art: `ClassCardRendererTest` covers card layout, not literals.
- **Cooldown precedence:** add two cases to the existing ability-trigger tests: low stacks plus a live cooldown returns `rejected()` with no announcement; low stacks off cooldown returns `fizzled("No Momentum")`. Prior art: `OffloadAbilityTest`, `OnslaughtAbilityTest`.
- **HP-scaled path:** add one test definition with a real `HpScaledPassive`, assert `cleanupFor` removes its modifier and the card grouping draws one family line. Prior art: `ClassSwitchCleanupTest` builds custom definitions, and `ClassPresentationTest` asserts card lines.
- **Round check:** the full suite stays green; client-visible changes ship through `./gradlew deployClient`; `latest.log` is grepped for `infusedclasses` errors; `tools/doc-flags.sh` and `tools/arch-flags.sh` run before the round is called done.

## Out of Scope

- Deleting the HP-scaled machinery. The guide retains it for a later kit; a separate deletion spec can take the audit's lean line if that kit is cancelled.
- Re-tuning class numbers or the x1.2 Gladius tradeoff.
- A gametest harness or automated client rendering tests.
- Making the card body clickable; the copy fix is the chosen fix.
- Allowing class switching at will.
- Changing ADR-0012's rule that the extra damage is dropped when the base hit kills.
- Changing the settle's bypass tags or exhaustion value.
- The prior audit's already-implemented fixes (post-mitigation pricing, crit particles, server monotonic clock, the `ServerEvents` split), unless a regression appears.
- The client HUD's local anchor; it stays local, only the clock source changes.

## Further Notes

- Audit mapping: the report's findings are picker authorization, lethal-hit state, settle scaling, client clock, mitigation test, command feedback, picker footer, cooldown precedence and HP-scaled retention. All nine are in this spec.
- The audit's `Lean: -220 lines` is deliberately not taken. The guide says the HP-scaled machinery stays; the spec keeps it and closes the test gap instead.
- In-game hand-checks: kill a mob with a full swing while Onslaught is live, with Impetus pending, and as a Gambler; check the stack price, window end, Impetus spend, Streak move and crit cue. Then a Hard and Easy mob hit on a Gladius Legionary, `/class set` feedback, the picker footer, and a low-stack cooldown press. Client-visible changes need `deployClient` and a `latest.log` read.
- The melee contracts from AGENTS.md still apply: a full-charge spear thrust earns like a sword, and a hit fully eaten by a raised shield earns nothing while Rush, Impetus and Rend stay pending. The settle hand-check also applies: a bonus smaller than the hit lands, the target loses hit plus bonus with one sound, hurt flash and knockback, and later hits compare against the combined total.
- The second-pick gate check needs a packet sender or a temporary dev-only path; do not ship a production command for it.

## Comments

Implemented on the working tree. The picker gate is `PickerGrants` plus the `ClassSwitch`
consume; join and both commands open through `ClassCommands.openPicker`. Killing blows share
the attacker shape and plan with non-lethal hits, with `LETHAL_DAMAGE` standing in for the
positive amount and the settle/victim feed skipped. `HitMitigation.landedAfterArmor` is the
pure seam; it starts after vanilla armor, since the armor enchantment check needs the live
entity and source. The settle type, client clock, command feedback, picker footer, cooldown
precedence and HP-scaled retention all landed with tests/docs.

`./gradlew test` is green and `./gradlew deployClient` copied the build to the live
`1.21.11` instance. In-game checks still owed: a cold `select_class` packet, the
kill-move matrix, Hard/Easy Gladius settle, `/class set` source feedback, the picker footer,
a cooling low-stack press, and a `latest.log` read from the new build.
