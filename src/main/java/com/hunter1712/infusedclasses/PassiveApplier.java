package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Adapter for {@link PassiveEffect}: makes the player's attributes agree with the decision. */
public final class PassiveApplier {
	private PassiveApplier() {
	}

	/**
	 * One tick for one player: the single reconcile home. Decay, then conditionals, then
	 * the HP-scaled pair, then the stance aura, then the wheel's window aura, then the
	 * Rush window's speed, in that order; the switch cleanup mirrors
	 * it in {@link #onClassChanged}. Stance and the Rush speed ride here (not in the entry tick) so the tick
	 * order and the write-only-on-change discipline are decided once. Idempotent: an
	 * attribute is only written when what is applied differs from what should be,
	 * so the modifier is created once and left alone while the stacks hold still.
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
		applyStance(player, def, runtime, now);
		applyRoulette(player, runtime, now);
		RushApplier.tick(player, runtime, now);
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
	 * Legionary stance auras: only the current stance's modifiers sit on the player,
	 * the other stance's come off. Non-Legionaries skip entirely. Both sides iterate
	 * the stance's single aura list, so write and remove cannot drift.
	 */
	private static void applyStance(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		if (!def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return;
		}
		for (PassiveEffect.Modifier wanted : RotateAbility.auraFor(runtime.stance())) {
			AttributeWriter.write(player, wanted);
		}
		for (PassiveEffect.Modifier stale : RotateAbility.auraFor(RotateAbility.flipped(runtime.stance()))) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
		if (runtime.isAequilibriumLive(now)) {
			for (PassiveEffect.Modifier wanted : AequilibriumAbility.windowModifiers()) {
				AttributeWriter.write(player, wanted);
			}
		} else {
			for (PassiveEffect.Modifier stale : AequilibriumAbility.windowModifiers()) {
				AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
			}
		}
	}

	/**
	 * Roulette window auras: Ward and Haste sit on the player only while their spin is
	 * live, and the write/remove sides iterate the same stat list, so they cannot drift.
	 * A spin that is over reads as no window: everything comes off. Non-Gamblers never
	 * arm the window, so they only ever meet the remove path.
	 */
	private static void applyRoulette(ServerPlayer player, PlayerRuntime runtime, long now) {
		RouletteAbility.Outcome live = runtime.rouletteOutcome(now);
		List<PassiveEffect.Modifier> wanted =
				live == null ? List.of() : RouletteAbility.statAuraFor(live);
		for (PassiveEffect.Modifier stat : RouletteAbility.allStatModifiers()) {
			if (wanted.contains(stat)) {
				AttributeWriter.write(player, stat);
			} else {
				AttributeWriter.remove(player, stat.attribute(), stat.modifierId());
			}
		}
	}

	/** Every stance/window modifier in one place: leaving Legionary takes them all off. */
	private static void removeStanceModifiers(ServerPlayer player) {
		for (PassiveEffect.Modifier stale : allModifiers()) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
	}

	/** Every wheel stat in one place: leaving the Gambler takes them all off. */
	private static void removeRouletteModifiers(ServerPlayer player) {
		for (PassiveEffect.Modifier stale : RouletteAbility.allStatModifiers()) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
	}

	/** The full set both sides draw from, so write and remove cannot drift apart. */
	private static List<PassiveEffect.Modifier> allModifiers() {
		List<PassiveEffect.Modifier> all = new ArrayList<>(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS));
		all.addAll(RotateAbility.auraFor(RotateAbility.Stance.SCUTUM));
		all.addAll(AequilibriumAbility.windowModifiers());
		return all;
	}

	/**
	 * Spender consume by class: resolves the priced passive by trigger and consumes it. The
	 * single path for Rend, so the entry never looks the passive up itself: the definition
	 * owns the one trigger scan, and the entry stays out.
	 */
	public static boolean consumeSpent(ServerPlayer player, PlayerRuntime runtime, ClassDefinition def) {
		return def != null
				&& consumeStacks(player, runtime, def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT));
	}

	/**
	 * Class switch: the new class's tick never visits the old passives, so their modifiers
	 * would orphan. Removes what the old class earned but the new one does not carry, drops
	 * the old stacks, and resets the cooldown: a new class starts empty.
	 */
	public static void onClassChanged(ServerPlayer player, PlayerRuntime runtime,
			ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef == newDef) {
			return;
		}
		if (oldDef != null) {
			for (ConditionalPassive passive : oldDef.passives()) {
				if (newDef == null || !newDef.passives().contains(passive)) {
					AttributeWriter.remove(player, passive.attribute(), passive.modifierId());
				}
			}
			// Vigor holds no stacks to clear, only the modifiers come off.
			for (HpScaledPassive hp : oldDef.hpPassives()) {
				if (newDef == null || !newDef.hpPassives().contains(hp)) {
					AttributeWriter.remove(player, hp.attribute(), hp.modifierId());
				}
			}
			// Stance auras are transient like Vigor: leaving the class takes them off.
			// The Legionary marker lives with the stance reconcile below, not with the passives.
			if (oldDef.hasAbility(ClassRegistry.ROTATE_ABILITY)
					&& (newDef == null || !newDef.hasAbility(ClassRegistry.ROTATE_ABILITY))) {
				removeStanceModifiers(player);
			}
			// Wheel stats are transient too: leaving the Gambler takes the live spin's auras off.
			if (oldDef.hasAbility(ClassRegistry.ROULETTE_ABILITY)
					&& (newDef == null || !newDef.hasAbility(ClassRegistry.ROULETTE_ABILITY))) {
				removeRouletteModifiers(player);
			}
			runtime.clear(oldDef);
		}
		runtime.resetCooldowns();
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
