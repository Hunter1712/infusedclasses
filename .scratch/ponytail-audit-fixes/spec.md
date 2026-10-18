# Spec: Ponytail Audit Fixes

Status: ready-for-agent

## Problem Statement

As the mod author, I ran a full-repo audit and it found three real defects, one small per-tick waste, and three stale doc lines. `/class set` can never accept a class id, so admins have no way to set or recover a class. Spear thrusts and mace smashes read a swing charge from the last ordinary swing, so the mod's whole full-swing rule silently misfires on two vanilla weapon families. A raised shield grants everything a landed hit grants — Momentum stacks, Streak movement, Rush's bonus, Impetus and Rend spends — while dealing no damage. And the docs tell people to run `gradle` (not installed here), grep a log that never sees the rejection line, and look for a jar version that no longer exists.

## Solution

Fix the three defects and the doc drift, and remove the duplicate per-tick state read. The class command takes a vanilla id argument so namespaced ids parse. The swing charge is captured on every melee damage path. The melee plan learns what the hit actually landed and earns nothing from a hit that took no damage. The server tick resolves one state read per player and hands it to both adapters. Docs and the build round checklist tell the truth.

## User Stories

1. As a server admin, I want `/class set <player> <class>` to accept the ids the picker and suggestions offer, so that setting or recovering a player's class works from chat.
2. As a server admin, I want every registered id to be the only thing the argument accepts, so that a typo fails with `Unknown or locked class` and leaves the previous choice untouched.
3. As a server admin, I want the tab suggestions to be directly usable text, so that tab-completing and pressing enter does not produce a parse error.
4. As a Juggernaut who fights with a spear, I want spear thrusts to count as full swings when I hold the attack, so that Momentum, Rush credit and Rend stacks behave like they do with a sword.
5. As a Mace player, I want mace smashes to count from their own charge, so that a fresh join does not earn nothing and poke spam does not count as full swings.
6. As a Gambler with a spear, I want the dice to roll from the thrust's own charge, so that crits, betrayals and Streak movement are not driven by an unrelated earlier sword swing.
7. As a PvP player facing a raised shield, I want a fully blocked swing to earn nothing, so that shields are not a free farm for Momentum armor and Streak.
8. As a Juggernaut in PvP, I want a blocked swing inside my Rush window to leave the bonus pending, so that the next hit that lands still gets it.
9. As a Legionary in Testudo, I want a blocked mob hit to pay no stance take mult, so that the shield does not feed the heal-back.
10. As a player fighting a mob whose hit was reduced by a shield, I want the stance and crit bonus to scale what actually landed, so that the numbers I see match the damage I took.
11. As a player, I want half swings to still earn nothing on every path, so that spam stays worthless.
12. As a server operator, I want the per-tick fan-out to resolve one state read per player, so that the server thread makes half as many small allocations on idle ticks.
13. As a player, I want the class rejection message to say where it appears, so that I look at my chat instead of asking an admin to grep a log.
14. As an admin, I want the FAQ to point at what the rejected player sees, so that I do not chase a log line that is never written.
15. As a fresh contributor, I want the install steps to run as written, so that `gradle build` is not a dead command on a machine that only has the wrapper.
16. As a fresh contributor, I want the install page to name the jar that a build actually produces, so that I do not look for `0.2.0`.
17. As a maintainer doing the game-update round, I want the two vanilla contracts the mixins bend written where the round checklist lives, so that a silent break is caught in game, not by a player report.
18. As a mod author, I want the audit's three defects closed without reopening any ADR seam, so that hit gating stays in the pure modules and the command keeps its direct registry call.
19. As the next implementer, I want the melee gate to arrive as a pure decision input pinned by `MeleePlanTest`, so that it has one home and one test seam.

## Implementation Decisions

- **The class command takes a vanilla id argument.** `ClassCommands` swaps its class argument to the vanilla identifier argument (`IdentifierArgument.id()`), so `infusedclasses:juggernaut` parses and the existing suggestions become directly usable. Validation stays where it is: `ClassSwitch.trySwitch` through `PlayerClasses`. ADR-0003's deferral is untouched — the argument selects among registered ids, it does not add a second source of classes, so the lookup-interface trigger does not fire. The commands guide page stops saying "one word".
- **The swing charge is captured on both melee damage paths.** `PlayerMixin` gains a second capture at the head of `stabAttack`, writing the same field as the existing `onAttack` capture. 1.21.11 spear thrusts (`PiercingWeapon`) and mace smashes (`KineticWeapon.damageEntities` from the mace's use tick) damage through `stabAttack`, which never calls `onAttack`; the capture is the fix, the mixin still holds zero policy. The consumption side (`MeleePlan` reading the charge, `SwingChargeAccess.of`) is unchanged.
- **`MeleePlan` learns what the hit landed.** `planHit` and `resolve` take the landed amount alongside the charge. A hit whose landed amount is zero earns nothing: no stack bump, no dice verdict or Streak movement, no Rush credit, no Impetus spend, no Rend payload spend. The plan early-returns before any peek that costs state, so a blocked hit reads no runtime state and materialises no victim runtime. `HitPlan` and `Landed` keep their shapes: an unearned hit lands nothing because its verdict is none, its Rend bonus is 0, and its mults are 1.0 against a zero base. Half-swing behaviour is unchanged.
- **The hook feeds the landed amount and settles from it.** The attacker branch passes the callback's post-shield amount (Fabric's `damageTaken`) into the plan and uses the same value as the stance/dice settle base; the victim branch swaps the same way, which makes a blocked mob hit a no-op through `DamageSettle.settleTake` already. The callback's `blocked` flag stays unused: the gate is "did any damage land", not "was a shield touched", so a partially blocked hit that still dealt damage keeps earning. This also moves the bonus base from pre-modifier to post-modifier for freezing- and helmet-scaled hits, which is the intended "what landed" basis.
- **The `lastHurt` trick is not touched.** The single guarded hurt in `DamageSettle` was verified against 1.21.11 `hurtServer` and stays as it is; only its manual check moves into the round checklist.
- **One state read per tick per player.** The server tick resolves one `PlayerRuntime.State` per classed player and passes it to `PassiveApplier.apply` and `StatusSync.maybeSend`, which forwards it to `PlayerStatusPayload.of`; neither adapter builds its own read. `StatusSync.sendNow` (join and respawn, off-tick) builds its own read as today. No wire shape changes.
- **Docs and checklist.** `AGENTS.md`'s build section gains the two in-game checks that guard the vanilla contracts (a full swing on a spear earns like a sword; a shielded hit pays nothing and leaves pending bonuses unspent). The `Unknown or locked class` javadoc says chat, and the FAQ tells players it appears on their screen, not in the log. The README and the installation page use `./gradlew` and either the current version or a version-free jar path.
- No new modules, no new seams: the one seam this spec adds to is the existing pure `MeleePlan`, and the command mixin and tick wiring carry no test seam, same as today.

## Testing Decisions

- Good tests here pin external behaviour of pure decisions with injected values and time — no game objects, no player or level fixtures, no mixin verification. Prior art: `MeleePlanTest` (plans and settles through injected charges and rolls), `DamageSettleTest` (pure settle math), `ClassSwitchCleanupTest` (classless and switch cleanup through the decision seam), `PlayerStatusTest` (snapshot builder at armed/ready/live/spent boundaries, `movedSince` for discrete moves), `ProtocolTest` (codec round trip).
- `MeleePlanTest` gains the blocked-hit cases at full charge: a zero landed amount earns no bump, no Rush credit, no Impetus spend, no Rend payload spend, no Streak movement and no verdict; a partially landed amount still earns and the combined mult is stated against the landed amount; a blocked hit inside a live Rush window leaves the bonus pending for the next landed hit; a blocked hit with a Rend window armed leaves the window armed. Half-charge cases stay as they are.
- `PlayerStatusTest` keeps pinning the snapshot boundaries, now through the passed-in state; `ProtocolTest` and `ClassSwitchCleanupTest` stay green unchanged.
- Not unit-tested, verified in game or by reading: both swing-charge captures, the `/class set` argument type (Brigadier parse needs a live command source for the player argument), the hook's branch values, the tick state threading's effect on allocation, and the checklist lines themselves.

## Out of Scope

- Rewriting the `DamageSettle` guarded-hurt trick or adding a Fabric gametest harness for it; the checklist line is the whole answer this round.
- A Brigadier command-tree test.
- Any change to vanilla-crit retirement, classless behaviour, the dice rules, or the wire payload.
- The HUD's countdown anchoring (a recorded decision, ADR-0017/0021) and the picker's per-frame string work.
- Docs beyond the three named items, icon/listing work, and the audit's remaining nice-to-haves.

## Further Notes

- Audit mapping: the three defects are findings 1–3 (command, charge, shield), the doc drift is findings 5–6, the tick read is finding 7, and finding 4 (the vanilla-contract checklist) rides along.
- ADRs read and respected: 0001 (decide purely, apply in an adapter), 0003 (class lookup deferral, trigger not fired), 0005 and 0007 (hit gating stays in the pure modules, the entry maps), 0012 (the mod owns crits), 0022 (one steady-state emission point), 0025 (the lane is the key).
- The glossary terms this spec leans on: swing charge, full swing, landed hit, lane, window, one-shot, Momentum, Rush, Rend, Impetus, Testudo, stance, Streak, crit/betrayal, class command, gamemaster.
- The two captures and the plan gate are the only behaviour changes; everything else is wiring, docs, or an allocation removed.

## Comments

Implemented. `/class set` takes `IdentifierArgument.id()`; the swing charge gains a second capture at the head of `stabAttack` beside the `onAttack` one; `MeleePlan.planHit`/`resolve` take the landed amount and return the empty plan at zero (five new `MeleePlanTest` cases pin it, the Impetus carry-over included); the hook passes Fabric's `damageTaken` into the plan and settles from it; the tick reads one `PlayerRuntime.State` and hands it to `PassiveApplier.apply` and `StatusSync.maybeSend`; `AGENTS.md` (both mixin contracts plus the `DamageSettle` guarded-hurt check), the commands page, FAQ, README, installation page, glossary and ADR-0003's example line are updated. Full suite green.

One correction to the spec's finding text: `stabAttack` itself never calls `onAttack`, but the spear's caller `PiercingWeapon.attack` does, after the damage lands. So a spear-only player read the previous thrust's charge (stale by one action), not zero forever; the mace smash really never reaches `onAttack`. The fix is unchanged — capture at the stab's head, the ticker the stab is about to use.

`IdentifierArgument` resolves a bare name into the `minecraft` namespace, so `/class set @s juggernaut` means `minecraft:juggernaut` and is rejected; only the namespaced `infusedclasses:juggernaut` (what the suggestions offer) works. The commands page documents that.
