# 01: Delete flat-stat pipeline & make ConditionalPassive fit

**What to build:** `Passive`, `ClassAttributes`, `ClassApplier`, `ServerModifiers`, `ModifierSink`, and their tests are gone. `ClassDefinition` now carries `List<ConditionalPassive>` with Warrior's one `Bulwark` conditional passive. Old passive tests removed or replaced.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] `Passive`, `ClassAttributes`, `ClassApplier`, `ServerModifiers`, `ModifierSink` records/classes removed.
- [x] `ClassDefinition` exposes `List<ConditionalPassive>` rather than old passives.
- [x] Warrior definition registers the `Bulwark` conditional passive with trigger `on_hit_taken`, max stacks 10.
- [x] Passive-specific tests removed or adapted.
- [x] Project compiles cleanly with no dead references.

## Comments

Implemented. `gradle test` green; `ClassAttributesTest`/`ModifierSinkTest` deleted, `ClassRegistryTest`/`ClassPresentationTest` adapted to `ConditionalPassive` data and the `CardLine` presentation seam.
