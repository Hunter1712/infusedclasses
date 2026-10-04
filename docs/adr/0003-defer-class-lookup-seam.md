# Class lookup stays a direct registry call until a second source of classes exists

## Status

Accepted. Deferred on purpose — this records a decision **not** to build something. See "Trigger"
below for the condition that reverses it.

## Context

An architecture review proposed abstracting class lookup behind an interface, so that callers ask for
a class rather than reaching for the registry.

There is exactly one thing that would implement it today: `ClassRegistry`, which holds every class as
a registration-time fact and answers by id. An interface with a single implementation and no second
caller is a seam in name only. It adds an indirection to every lookup, and it hides the fact that the
registry is currently the single source of truth rather than one interchangeable source.

The cost of building it is small. The cost of not building it, once a second source exists, is that
every caller has to be revisited at the moment the change is already load-bearing.

## Decision

No lookup interface. Callers reach `ClassRegistry` directly.

## Trigger

Build the lookup interface **the moment a second source of classes appears**, not before. Any of these
qualifies:

- classes defined outside the registry — a config file, a datapack or data-driven JSON, a command
  argument that adds one
- a second registry or provider consulted alongside `ClassRegistry`
- a caller that must supply classes from outside the registry, including a test double

At that point the lookup is the thing that varies, and the interface stops being hypothetical. Note
that this is a *different* seam from the one in `0001-decide-purely-apply-in-an-adapter`: that ADR
assumes exactly one adapter per decision, so the rule here follows from it rather than contradicting
it.

## Consequences

- `/improve-codebase-architecture` should treat this as settled. Proposing "abstract class lookup
  behind a seam" again is re-proposing a decision that was already made, and should be answered by
  pointing here.
- The trigger is the review's job to watch for. If classes ever stop being a registration-time fact,
  this ADR is the place to record that the trigger fired and what the second source turned out to be.