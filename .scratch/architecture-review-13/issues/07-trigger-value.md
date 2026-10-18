# 07 — The trigger is a value, not a string

Status: done

## Context

`ConditionalPassive` carries `String trigger`, matched by `equals` in `PassiveStacks` and
`ClassDefinition.passiveForTrigger`, with one constant (`ON_HIT_DEALT`) and a
`TriggerPhrasing` switch whose `default` arm invents copy ("Trigger" / "no trigger") for any
trigger it does not know. A typo compiles; an unrecognised trigger silently earns nothing
(the ADR-0005 failure mode) and describes itself with words no ability owns. Round-13
candidate 7.

## Deliverable

- `Trigger` (new): a record of `id`, `earn` and `idle` copy, with `ON_HIT_DEALT` as the one
  production constant. The copy rides the trigger, so a new trigger adds one constant with
  its own words and cannot describe itself with copy it does not carry.
- `ConditionalPassive.trigger()` is a `Trigger`; `TriggerPhrasing`, the `phrasing()` switch
  and the string constant die.
- `PlayerRuntime.onTrigger` / `PassiveStacks.onTrigger` / `ClassDefinition.passiveForTrigger`
  take a `Trigger`.

## Done when

- Picker copy is unchanged for Momentum (`Land a hit: … 5s no hit: lose all.`), pinned by
  `ClassRegistryTest`.
- Tests that build a second trigger carry its copy themselves
  (`new Trigger("on_land", "Land a swing", "no swing")`); nothing in `src/main` builds a
  trigger outside `Trigger.ON_HIT_DEALT`.
