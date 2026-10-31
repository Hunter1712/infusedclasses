# ADR-0033: One Fabric module per supported Minecraft version

Status: accepted

## Context

The mod targeted a single Minecraft version (1.21.11). Players on 1.20.1, 1.21.1 or the 26.x
line could not install it, and every piece of project metadata, docs and tooling said
"1.21.11 only". A single Fabric jar can only target one Minecraft version — a jar's mappings,
mixins, networking codecs, damage hooks and client widgets are bound to the game it was built
against — so a wider version range is not a build change, it is a structural one.

A naive answer is to fork the repo per version. That duplicates the gameplay decisions
(Momentum's checkpoint curve, the Legionary stance quad, the Roulette bands, the settle math)
three times, and balance becomes three independent codebases that drift within one release
cycle. The other naive answer — one module, `if (version)` at every adapter seam — makes every
version-coupled difference visible in the shared source, which is exactly what ADR-0001's
seam rule ("decisions stay pure, adapters apply them") already argues against.

The version-coupled surface is real but bounded: identifier type naming, save-data format,
mixin targets and signatures, networking API, damage and hurt APIs, attribute writer keys,
client screen/widget signatures, HUD registration. The decision surface — every rule test,
roster, bar, plan, palette and layout — has no Minecraft import at all.

## Decision

1. **The build is multi-module.** A plain Java library `common` compiles the version-agnostic
   decisions to Java 17 bytecode (the lowest runtime any target needs), and one Fabric module
   per supported version (`versions/1.20.1`, `versions/1.21.1`, `versions/26.3`, Gradle names
   `:mc1201`, `:mc1211`, `:mc263`) applies them. A shared `gradle/version-module.gradle` holds
   the module template; each module's own `gradle.properties` declares its Minecraft version,
   Fabric API build, Java release and mappings kind.

2. **`common` holds no Minecraft import.** The extraction rule is mechanical: a class and its
   transitive type references move to `common` only when none of them import `net.minecraft`.
   What remains coupled (class registry, ability modules, attribute writer, payloads, mixins,
   damage hooks, client screens and HUD, server entry) stays per module. Tests follow their
   class: pure tests run once in `common`, tests that touch a coupled type run per module.

   **Where the rule stops.** A decision can need no game object at all and still fail the
   rule, because a *value type* (`net.minecraft.resources.Identifier` on an id,
   `net.minecraft.resources.ResourceKey<Attribute>` in `PassiveEffect.Modifier`) is an
   import too. The whole melee/runtime/crit path sits on that edge: `ClassDefinition.id`,
   the per-ability modifier ids, the ability enum shapes and `ChatFormatting` on a
   `ChatLine`. Those are not game objects — ADR-0001 would call them a decision — yet the
   multi-module rule keeps them per version, so `MeleePlan`, `PlayerRuntime`,
   `ConditionalPassive`, `CritRule`/`CritRoll`, `BindMorph` and `PassiveStacks` are
   currently triplicated, byte-identical. This is the honest cost of the scaffold landing
   first: the mechanical rule buys the leaf decisions (`CardPalette`, `Lane`, `LaneRead`,
   `Trigger`, `Cooldowns`, `FullSwing`, `PickerGrants`), and the rest waits on lifting that
   id/attribute/style vocabulary into `common`-side plain data — see ticket 02's remaining
   work.

3. **Each version module owns its adapters, not its own copy of the rules.** Identifier naming
   (`Identifier` on 1.21.11 and 26.3, `ResourceLocation` earlier), save data (`ValueInput`/
   `ValueOutput` vs `CompoundTag`), attribute modifier keys (`ResourceLocation` vs a
   `UUID.nameUUIDFromBytes` of it), the melee hook (Fabric's after-damage event vs a
   `LivingEntityMixin` on 1.20.1), the vanilla-crit retirement (`canCriticalAttack` on
   1.21.11 and 26.3 vs the inline `1.5F` in `attack` plus `crit`/`magicCrit` on 1.20.1 and
   1.21.1), networking (`PayloadTypeRegistry` vs `PacketType` vs `CustomPacketPayload`) and
   client rendering (`GuiGraphics` vs `GuiGraphicsExtractor`, `HudElement.render` vs
   `extractRenderState`) all adapt inside the module that needs them.

4. **One jar per version, named by it.** Each module produces `infusedclasses-<mod>+<mc>.jar`
   declaring `~<mc>` and its own Java floor, so a jar for the wrong version is rejected at
   launch rather than misbehaving, and a modpack author picks an artifact by suffix.

5. **Tooling follows the module it serves.** `mc-jar.sh` and `mixin-targets.sh` take a version
   and default to each module's declared version; `arch-flags.sh` and `doc-flags.sh` scan the
   module sources; each module's `deployClient` defaults to its own launcher instance.

## Consequences

- A gameplay change is made once in `common` and ships to all three versions — for the classes
  that have cleared the rule. For the decision core still on the value-type edge (see point 2),
  a change is made three times today, which is why the remaining extraction is tracked, not
  claimed.
- Adding a version is a new directory plus a port, not a fork; dropping one is a directory
  deletion. Removing 1.21.11 is therefore a deletion, not a deprecation.
- Each module must compile and test independently, so a breakage in one target does not block
  the others (`./gradlew :mc1211:test` works alone).
- Test cost roughly triples: the pure tests run in `common` once, but the coupled tests run per
  module. 1.20.1 additionally boots vanilla registries in its test JVM (`MinecraftTestBootstrap`),
  because its `Registries` static init reaches `BuiltInRegistries`.
- In-game validation is manual and per version — see the spec's checklist. No automated UI test
  is attempted.

## Superseded points

None.

## Related

- [0001 — Decide purely, apply in an adapter](../adr/0001-decide-purely-apply-in-an-adapter.md) —
  this ADR extends that seam across versions: the adapter is the version module.
- Spec: `.scratch/multi-version-support/spec.md`.
