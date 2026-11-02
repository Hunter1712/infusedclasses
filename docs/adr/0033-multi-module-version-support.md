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
   per supported version (`versions/1.21.1`, `versions/26.3`, Gradle names `:mc1211`, `:mc263`)
   applies them. A shared `gradle/version-module.gradle` holds
   the module template; each module's own `gradle.properties` declares its Minecraft version,
   Fabric API build, Java release and mappings kind.

2. **`common` holds no Minecraft import.** The extraction rule is mechanical: a class and its
   transitive type references move to `common` only when none of them import `net.minecraft`.
   What remains coupled (attribute writer, mixins, networking payloads, damage hooks, client
   screens and HUD, server entry) stays per module. Tests follow their class: pure tests run
   once in `common`, tests that touch a coupled type run per module.

   **Where the rule stops.** A decision can need no game object at all and still fail the rule,
   because a *value type* (`net.minecraft.resources.Identifier` on an id,
   `net.minecraft.resources.ResourceKey<Attribute>` in `PassiveEffect.Modifier`) is an import
   too. The whole melee/runtime/crit path sat on that edge, so the vocabulary was lifted into
   `common`-side plain data: `ModIds` for ids, `AttributeRef` for the attribute a modifier
   lands on, `LineStyle` for a chat style, and `StatusView` as the seam the HUD reads a
   snapshot through. With those in place the decision core — class registry and definitions,
   every ability module, the conditional passives, `MeleePlan`, `PlayerRuntime`,
   `CritRule`/`CritRoll`, `BindMorph`, `AbilityRoster`, `PassiveSweep`, `ClassPresentation`,
   `CardLayout`/`CardWrap`, `ClassInfo`, `WindowFamily` and `ChatLine` — lives once in
   `common` with its tests.

   What still stays per module is coupled for a structural reason rather than a value one:
   the status snapshot owns the wire codec, and the two classes that consume it wholesale
   (`StatusHud`, `StatusStore`) go with it. That is a documented boundary, not an oversight.

3. **Each version module owns its adapters, not its own copy of the rules.** Identifier naming
   (`Identifier` on 26.3, `ResourceLocation` earlier), save data (`ValueInput`/
   `ValueOutput` vs `CompoundTag`), attribute modifier keys (`ResourceLocation` vs a
   `UUID.nameUUIDFromBytes` of it), the melee hook (Fabric's after-damage event vs a
   `LivingEntityMixin` where a version needs one), the vanilla-crit retirement (the inline `1.5F` in `attack`
   plus `crit`/`magicCrit`, since no target exposes a cancellable `canCriticalAttack`),
   networking (`PayloadTypeRegistry` vs `PacketType` vs `CustomPacketPayload`) and client
   rendering (`GuiGraphics` vs `GuiGraphicsExtractor`, `HudElement.render` vs
   `extractRenderState`) all adapt inside the module that needs them.

   Byte-identical adapters and their tests live once under `versions/shared` and compile
   into each module against that module's mappings; a file that needs a version-specific
   API moves back into its module. 1.20.1 was removed by ADR-0034; 1.21.1 and 26.3 remain.

4. **One jar per version, named by it.** Each module produces `infusedclasses-<mod>+<mc>.jar`
   declaring `~<mc>` and its own Java floor, so a jar for the wrong version is rejected at
   launch rather than misbehaving, and a modpack author picks an artifact by suffix.

5. **Tooling follows the module it serves.** `mc-jar.sh` and `mixin-targets.sh` take a version
   and default to each module's declared version; `arch-flags.sh` and `doc-flags.sh` scan the
   module sources; each module's `deployClient` defaults to its own launcher instance.

## Consequences

- A gameplay change is made once in `common` and ships to both versions; a version-coupled
  change is made once in the one module that needs it. A balance or rule edit touches one file.
- Adding a version is a new directory plus a port, not a fork; dropping one is a directory
  deletion. Removing 1.21.11 is therefore a deletion, not a deprecation.
- Each module must compile and test independently, so a breakage in one target does not block
  the others (`./gradlew :mc1211:test` works alone).
- Test cost roughly doubles: the pure tests run in `common` once, but the coupled tests run per
  module. In-game validation is manual and per version — see the spec's checklist. No automated UI test
  is attempted.

## Superseded points

None.

## Related

- [0001 — Decide purely, apply in an adapter](../adr/0001-decide-purely-apply-in-an-adapter.md) —
  this ADR extends that seam across versions: the adapter is the version module.
- Spec: `.scratch/multi-version-support/spec.md`.
