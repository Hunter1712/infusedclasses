# Coding standards

Judgement calls the reviewer enforces. Mechanical checks live in CI
(`.github/workflows/build.yml` runs `gradle build`, which includes `test`).

## Docs move with behavior

A change that alters player-visible behavior, module responsibilities, or domain
vocabulary updates `GLOSSARY.md` and the originating spec under `.scratch/` in
the same change. Code that contradicts the glossary is a defect, whichever side
is wrong — fix the stale side, don't leave both standing.

## Shared paths stay single

When a helper owns a state change plus its side effects, every caller routes
through it — never re-inline the inner call. A second call site that repeats
the inner half silently drops the outer half.
