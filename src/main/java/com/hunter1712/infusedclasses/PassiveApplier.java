package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Adapter for {@link PassiveEffect}: makes the player's attributes agree with the decision. */
public final class PassiveApplier {
	private PassiveApplier() {
	}

	/**
	 * One window-aura family: the marker ability that owns it, every modifier it can
	 * write, and the pure decision of what its live windows want. The reconcile walks
	 * the family once, so the write and remove sides cannot drift, and a class without
	 * the marker sweeps whatever the family could have written.
	 */
	private record WindowFamily(
			Identifier marker,
			List<PassiveEffect.Modifier> all,
			Function<PlayerRuntime.State, List<PassiveEffect.Modifier>> wanted) {
	}

	private static final List<WindowFamily> FAMILIES = List.of(
			new WindowFamily(ClassRegistry.ROTATE_ABILITY, stanceModifiers(), PassiveApplier::stanceWanted),
			new WindowFamily(ClassRegistry.ROULETTE_ABILITY, RouletteAbility.allStatModifiers(), PassiveApplier::rouletteWanted),
			new WindowFamily(ClassRegistry.RUSH_ABILITY, List.of(RushAbility.SPEED), PassiveApplier::rushWanted));

	/**
	 * One tick for one player: the single reconcile home. Decay, then conditionals, then
	 * the HP-scaled pair, then the window-aura families (the stance and its Aequilibrium
	 * window, the wheel's live spin, the Rush window's speed), in that order; the switch's
	 * cleanup takes off what {@link #cleanupFor} decides is stale. The tick order and the
	 * write-only-on-change discipline are decided once. Idempotent: an attribute is only
	 * written when what is applied differs from what should be, so the modifier is
	 * created once and left alone while the stacks hold still.
	 */
	public static void apply(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		if (def == null) {
			return;
		}
		Set<ConditionalPassive> moved = runtime.tickDecayMoved(def, now);
		for (ConditionalPassive passive : def.passives()) {
			settlePlan(player, PassiveEffect.planFor(passive, runtime.stacks(passive), moved.contains(passive)));
		}
		// Vigor: no stacks, no grace. Recomputed from live HP every tick; the writer's
		// early-out already covers no-op ticks, bucket 0 removes like dead stacks.
		int bucket = HpScaledPassive.bucketForCurrent(player.getHealth(), player.getMaxHealth());
		for (HpScaledPassive hp : def.hpPassives()) {
			settlePlan(player, PassiveEffect.planForBucket(hp, bucket));
		}
		reconcileWindows(player, def, runtime.state(now));
	}

	/** One reconcile pass over every family: a held marker writes what its state wants, an absent one wants nothing. */
	private static void reconcileWindows(ServerPlayer player, ClassDefinition def, PlayerRuntime.State state) {
		for (WindowFamily family : FAMILIES) {
			List<PassiveEffect.Modifier> wanted = def.hasAbility(family.marker())
					? family.wanted().apply(state) : List.of();
			for (PassiveEffect.Modifier modifier : family.all()) {
				if (wanted.contains(modifier)) {
					AttributeWriter.write(player, modifier);
				} else {
					AttributeWriter.remove(player, modifier.attribute(), modifier.modifierId());
				}
			}
		}
	}

	/** The stance family's wanted set: the current stance's aura, plus Aequilibrium's while its window is live. */
	static List<PassiveEffect.Modifier> stanceWanted(PlayerRuntime.State state) {
		List<PassiveEffect.Modifier> wanted = new ArrayList<>(RotateAbility.auraFor(state.stance()));
		if (state.lane(Lane.OATH).windowRemainingMs() > 0) {
			wanted.addAll(AequilibriumAbility.windowModifiers());
		}
		return wanted;
	}

	/** The wheel family's wanted set: the live spin's stat aura, empty when no spin is live. */
	static List<PassiveEffect.Modifier> rouletteWanted(PlayerRuntime.State state) {
		RouletteAbility.Outcome outcome = state.rouletteOutcome();
		return outcome == null ? List.of() : RouletteAbility.statAuraFor(outcome);
	}

	/** The Rush family's wanted set: the speed stat while the engage window is live. */
	static List<PassiveEffect.Modifier> rushWanted(PlayerRuntime.State state) {
		return state.lane(Lane.RUSH).windowRemainingMs() > 0 ? List.of(RushAbility.SPEED) : List.of();
	}

	/** The stance family's full set: both stances' auras plus the window pair, so write and remove cannot drift. */
	private static List<PassiveEffect.Modifier> stanceModifiers() {
		List<PassiveEffect.Modifier> all = new ArrayList<>(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS));
		all.addAll(RotateAbility.auraFor(RotateAbility.Stance.SCUTUM));
		all.addAll(AequilibriumAbility.windowModifiers());
		return all;
	}

	/**
	 * Applies a single passive effect plan: write the wanted modifier, remove the stale one,
	 * or skip. The adapter owns the write/remove/skip decision so the caller only computes plans.
	 */
	private static void settlePlan(ServerPlayer player, PassiveEffect.Plan plan) {
		switch (plan) {
			case PassiveEffect.Plan.Write(var wanted) -> AttributeWriter.write(player, wanted);
			case PassiveEffect.Plan.Remove(var attribute, var modifierId) -> AttributeWriter.remove(player, attribute, modifierId);
			case PassiveEffect.Plan.Skip() -> {
			}
		}
	}

	/**
	 * Spender consume by class: resolves the priced passive by trigger and consumes it. The
	 * single path for Rend, so the entry never looks the passive up itself: the definition
	 * owns the one trigger scan, and the entry stays out.
	 */
	public static boolean consumeSpent(ServerPlayer player, PlayerRuntime runtime, ClassDefinition def) {
		return def != null
				&& consumeStacks(player, runtime, def.passiveForTrigger(Trigger.ON_HIT_DEALT));
	}

	/**
	 * Class switch: the new class's tick never visits the old passives, so their modifiers
	 * would orphan. Removes every modifier {@link #cleanupFor} says is stale, drops the old
	 * stacks, and resets the runtime: a new class starts empty. The rule is a decision, so
	 * what a switch takes off is assertable without a player.
	 */
	public static void onClassChanged(ServerPlayer player, PlayerRuntime runtime,
			ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef == newDef) {
			return;
		}
		for (PassiveEffect.Modifier stale : cleanupFor(oldDef, newDef)) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
		runtime.clear(oldDef);
		runtime.resetAll();
	}

	/**
	 * What a switch must take off: every modifier the old class's kit could write — its
	 * stack passives, its Vigor pair, and the whole list of every window-aura family it
	 * marks — minus every modifier id the new class writes. One entry per id, so a kit
	 * naming an id twice removes it once; keyed by id, so a class that re-earns a modifier
	 * keeps it and the next tick rewrites its value, and a family both classes mark is left
	 * to the family's own sweep. Pure: the wiring only removes.
	 */
	static List<PassiveEffect.Modifier> cleanupFor(ClassDefinition oldDef, ClassDefinition newDef) {
		Set<Identifier> stillWritten = new HashSet<>();
		for (PassiveEffect.Modifier kept : writtenBy(newDef)) {
			stillWritten.add(kept.modifierId());
		}
		Set<Identifier> staleIds = new HashSet<>();
		List<PassiveEffect.Modifier> stale = new ArrayList<>();
		for (PassiveEffect.Modifier written : writtenBy(oldDef)) {
			if (!stillWritten.contains(written.modifierId()) && staleIds.add(written.modifierId())) {
				stale.add(written);
			}
		}
		return stale;
	}

	/**
	 * Every modifier a class's kit can put on the player: its stack passives, its Vigor
	 * pair, and the whole modifier list of each window family whose marker it holds — the
	 * same sets the tick can write, read through the same mapping. Empty for no class.
	 */
	private static List<PassiveEffect.Modifier> writtenBy(ClassDefinition def) {
		if (def == null) {
			return List.of();
		}
		List<PassiveEffect.Modifier> all = new ArrayList<>();
		for (ConditionalPassive passive : def.passives()) {
			all.add(PassiveEffect.forStacks(passive, 1));
		}
		for (HpScaledPassive hp : def.hpPassives()) {
			all.add(PassiveEffect.forBucket(hp, 1));
		}
		for (WindowFamily family : FAMILIES) {
			if (def.hasAbility(family.marker())) {
				all.addAll(family.all());
			}
		}
		return all;
	}

	/**
	 * Pure stack drop: clears one passive's stacks and timestamp, reporting whether
	 * anything was dropped. The JUnit-reachable half of the spender consume; the
	 * attribute removal rides with the player in {@link #consumeSpent}.
	 */
	static boolean dropStacks(PlayerRuntime runtime, ConditionalPassive passive) {
		if (passive == null || runtime.stacks(passive) <= 0) {
			return false;
		}
		runtime.clear(passive);
		return true;
	}

	/**
	 * Spender consume: drops the one passive's stacks and timestamp and takes the armor they
	 * earned off the player. Reports whether anything was consumed. Nothing held means
	 * nothing done. Only the priced passive is touched: a second stacker on the class
	 * keeps every stack.
	 */
	private static boolean consumeStacks(ServerPlayer player, PlayerRuntime runtime, ConditionalPassive passive) {
		if (!dropStacks(runtime, passive)) {
			return false;
		}
		AttributeWriter.remove(player, passive.attribute(), passive.modifierId());
		return true;
	}
}
