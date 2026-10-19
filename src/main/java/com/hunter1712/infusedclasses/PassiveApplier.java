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
			new WindowFamily(ClassRegistry.OFFLOAD_ABILITY, OffloadAbility.windowModifiers(), PassiveApplier::offloadWanted));

	/**
	 * One tick for one player: the single reconcile home. Bleed, then the tiered bars,
	 * then the always-on chassis, then the HP-scaled pair, then the window-aura families
	 * (the stance and its Aequilibrium window, the wheel's live spin, Offload's loosened
	 * window), in that order; the switch's cleanup takes off what {@link #cleanupFor}
	 * decides is stale. The tick order and the write-only-on-change discipline are
	 * decided once. The caller owns the tick's one state read and passes it: the bleed
	 * touches no window or lane, so the read is the same before or after it. Idempotent:
	 * an attribute is only written when what is applied differs from what should be, so
	 * the modifier is created once and left alone while the bar holds still.
	 */
	public static void apply(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime,
			PlayerRuntime.State state, long now) {
		if (def == null) {
			return;
		}
		runtime.decay(def, now);
		for (ConditionalPassive passive : def.passives()) {
			// Momentum owns the one tier-to-modifier table today. Another bar adds its
			// own module and a dispatch here; applying Momentum's ids to it would leak
			// the wrong rows onto the player and into the switch cleanup.
			if (!Momentum.PASSIVE.equals(passive)) {
				continue;
			}
			// The bar is read, never remembered: losing or spending stacks re-reconciles
			// to the row the bar names now, and below the first checkpoint the sweep takes
			// the row off entirely.
			reconcile(player, Momentum.allModifiers(), Momentum.forStacks(runtime.stacks(passive)));
		}
		for (ClassDefinition.AlwaysOn alwaysOn : def.alwaysOn()) {
			for (PassiveEffect.Modifier modifier : alwaysOn.modifiers()) {
				AttributeWriter.write(player, modifier);
			}
		}
		// No stacks, no grace: recomputed from live HP every tick; the writer's early-out
		// already covers no-op ticks, bucket 0 removes like a dead bar.
		int bucket = HpScaledPassive.bucketForCurrent(player.getHealth(), player.getMaxHealth());
		for (HpScaledPassive hp : def.hpPassives()) {
			settlePlan(player, PassiveEffect.planForBucket(hp, bucket));
		}
		reconcileWindows(player, def, state);
	}

	/** One reconcile pass over every family: a held marker writes what its state wants, an absent one wants nothing. */
	private static void reconcileWindows(ServerPlayer player, ClassDefinition def, PlayerRuntime.State state) {
		for (WindowFamily family : FAMILIES) {
			List<PassiveEffect.Modifier> wanted = def.hasAbility(family.marker())
					? family.wanted().apply(state) : List.of();
			reconcile(player, family.all(), wanted);
		}
	}

	/**
	 * The one reconcile pass: write every wanted modifier, then take off every sweep id
	 * the wanted set does not name. Keyed by id, so two rows sharing an id write once and
	 * a row that changed its amount overwrites in place instead of remove-then-write.
	 */
	private static void reconcile(ServerPlayer player, List<PassiveEffect.Modifier> all,
			List<PassiveEffect.Modifier> wanted) {
		for (PassiveEffect.Modifier modifier : wanted) {
			AttributeWriter.write(player, modifier);
		}
		for (PassiveEffect.Modifier modifier : all) {
			if (!names(wanted, modifier.modifierId())) {
				AttributeWriter.remove(player, modifier.attribute(), modifier.modifierId());
			}
		}
	}

	/** Whether any modifier in the list carries the id. */
	private static boolean names(List<PassiveEffect.Modifier> modifiers, Identifier id) {
		for (PassiveEffect.Modifier modifier : modifiers) {
			if (modifier.modifierId().equals(id)) {
				return true;
			}
		}
		return false;
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

	/** The Offload family's wanted set: the haste and speed pair while the loosened window is live. */
	static List<PassiveEffect.Modifier> offloadWanted(PlayerRuntime.State state) {
		return state.lane(Lane.OFFLOAD).windowRemainingMs() > 0
				? OffloadAbility.windowModifiers() : List.of();
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
	 * tiered bars, its always-on rows, its HP-scaled pair, and the whole list of every
	 * window-aura family it marks — minus every modifier id the new class writes. One
	 * entry per id, so a kit naming an id twice removes it once; keyed by id, so a class
	 * that re-earns a modifier keeps it and the next tick rewrites its value, and a family
	 * both classes mark is left to the family's own sweep. Pure: the wiring only removes.
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
	 * Every modifier a class's kit can put on the player: its tiered bars, its always-on
	 * rows, its HP-scaled pair, and the whole modifier list of each window family whose
	 * marker it holds — the same sets the tick can write, read through the same mapping.
	 * Empty for no class.
	 */
	private static List<PassiveEffect.Modifier> writtenBy(ClassDefinition def) {
		if (def == null) {
			return List.of();
		}
		List<PassiveEffect.Modifier> all = new ArrayList<>();
		for (ConditionalPassive passive : def.passives()) {
			if (Momentum.PASSIVE.equals(passive)) {
				all.addAll(Momentum.allModifiers());
			}
		}
		for (ClassDefinition.AlwaysOn alwaysOn : def.alwaysOn()) {
			all.addAll(alwaysOn.modifiers());
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
}
