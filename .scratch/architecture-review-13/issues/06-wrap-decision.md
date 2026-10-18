# 06 — The wrap is a decision

Status: done

## Context

`ClassCardRenderer.wrapRows` fuses the greedy wrap and the tone-boundary derivation to a live
`Font`: the string measuring is the only reason the decision cannot be asserted, so the card's
body layout (which words land on which row, where the tone boundary falls inside each row,
what the scroll range is) is pinned only through `descStartFor`'s pure fragment and
`CardLayout`'s rect math. Round-13 candidate 6.

## Deliverable

- `wrapRows(ClassPresentation.CardLine line, int wrapW, ToIntFunction&lt;String&gt; width)` and
  `measure(CardLayout.CardRect card, ClassPresentation.CardModel model, ToIntFunction&lt;String&gt; width)`;
  the widths arrive as a function of the string (the ticket said `IntUnaryOperator`, which cannot
  take a string — `ToIntFunction&lt;String&gt;` is what `font::width` matches), the renderer passes
  `font::width`.
- A test drives both with a stub measure (one pixel per character) and pins: wrapping only at
  word boundaries, the tone boundary on row one and zero after, a name wider than the card
  still starting row one, the empty-description line staying a single row, and the row count
  the scroll layout is built from.

## Done when

- `ClassCardRenderer` mentions `Font` only in the draw passes; `ClassSelectScreen` passes
  `font::width` and nothing else changes on screen.
- Every existing card test passes unchanged.
