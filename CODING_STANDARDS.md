# Coding standards

Judgement calls the reviewer enforces. Mechanical checks live in CI
(`.github/workflows/build.yml` runs `gradle build`, which includes `test`).

## Docs move with behavior

A change that alters player-visible behavior, module responsibilities, or domain
vocabulary updates `GLOSSARY.md` and the originating spec under `.scratch/` in
the same change. Code that contradicts the glossary is a defect, whichever side
is wrong — fix the stale side, don't leave both standing. Comments that state
behavior are docs too: a comment whose rationale the change overturns is
updated in the same change.

## Shared paths stay single

When a helper owns a state change plus its side effects, every caller routes
through it — never re-inline the inner call. A second call site that repeats
the inner half silently drops the outer half.

## Entries map, adapters apply

The server entry never writes player state: no effects, attributes, health, or
particles. It decides through pure modules, applies through adapters, and sends
signals. One result maps to one apply call; an entry that reaches past the
adapter re-derives what the adapter owns.

## Passives are addressed by trigger, never by position

Production code finds a passive by its trigger id, never by list index: a
`get(0)` on a passive list is a defect in `src/main`. Reordering must be a
no-op. Tests pinning a known registry shape may index; the game may not.
