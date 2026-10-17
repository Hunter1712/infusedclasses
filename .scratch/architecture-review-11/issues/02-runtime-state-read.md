# 02 — PlayerRuntime behind one state read

Status: done

## Context

`PlayerRuntime` is 418 lines / 44 public members, ~29 of them one-line forwards over
`Cooldowns` and its windows; the snapshot builder crosses 19 of them. Round-10 flag 4
said: a second consumer of the same shape means one `read(..., now)` seam instead of
per-window verbs. The second consumer arrived (payload, `/class info`, melee plan).
Round-11 candidate 2, the top recommendation.

## Deliverable

- `PlayerRuntime.State` record carrying the 19 transient facts at one clock:
  six lane remainders, six window remainders with their load flags, `flipTarget`,
  `rendBonusStacks`, `stance`, `streak`.
- `state(now)` is the one read. Every read verb dies (`rotateRemaining`,
  `rushWindowRemaining`, `hasRushBonusPending`, `isTestudoBuffLive`, `rouletteOutcome`,
  `streak`, `flipTarget`, …); arms, consumes, movement verbs and the passive-stack
  verbs stay verbs.
- Consumers move to the state: `PlayerStatusPayload.of`, `ClassInfo`, `MeleePlan`
  (`planHit`, `dealWithImpetus`, `takeFor`), `RouletteAbility.dealMult`, all six
  `fire` methods, `PassiveApplier`, and the tests.

## Done when

- `state()` is the only transient read on the interface; `gradle test` green with the
  window/grace boundaries re-pinned through the state
  (`PlayerRuntimeDeepeningTest` rewritten).
