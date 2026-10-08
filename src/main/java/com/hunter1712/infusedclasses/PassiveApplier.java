package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Adapter for {@link PassiveEffect}: makes the player's attributes agree with the decision. */
public final class PassiveApplier {
	private PassiveApplier() {
	}

	/**
	 * One tick for one player: decay stacks, then make the attributes agree with the plan.
	 * Idempotent: an attribute is only written when what is applied differs from what should be,
	 * so the modifier is created once and left alone while the stacks hold still.
	 */
	public static void apply(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		if (def == null) {
			return;
		}
		Set<ConditionalPassive> moved = runtime.tickDecayMoved(def, now);
		for (ConditionalPassive passive : def.passives()) {
			switch (PassiveEffect.planFor(passive, runtime.stacks(passive), moved.contains(passive))) {
				case PassiveEffect.Plan.Write(var wanted) -> AttributeWriter.write(player, wanted);
				case PassiveEffect.Plan.Remove(var attribute, var modifierId) -> AttributeWriter.remove(player, attribute, modifierId);
				case PassiveEffect.Plan.Skip() -> {
				}
			}
		}
		// Vigor: no stacks, no grace. Recomputed from live HP every tick; the writer's
		// early-out already covers no-op ticks, bucket 0 removes like dead stacks.
		int bucket = HpScaledPassive.bucketForCurrent(player.getHealth(), player.getMaxHealth());
		for (HpScaledPassive hp : def.hpPassives()) {
			PassiveEffect.Modifier wanted = PassiveEffect.forBucket(hp, bucket);
			if (wanted != null) {
				AttributeWriter.write(player, wanted);
			} else {
				AttributeWriter.remove(player, hp.attribute(), hp.modifierId());
			}
		}
		// Bonus pool: flat max-HP per class. The write early-out
		// covers no-op ticks; a zero bonus removes, so the tick self-heals within one
		// tick of any switch — no onClassChanged handling needed.
		if (def.maxHealthBonus() > 0) {
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.MAX_HEALTH, ClassRegistry.BONUS_HEALTH_ID, def.maxHealthBonus()));
		} else {
			AttributeWriter.remove(player, ClassRegistry.MAX_HEALTH, ClassRegistry.BONUS_HEALTH_ID);
		}
		StanceApplier.applyStance(player, def, runtime, now);
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
			if (oldDef.hasAbility(ClassRegistry.ROTATE_ABILITY)
					&& (newDef == null || !newDef.hasAbility(ClassRegistry.ROTATE_ABILITY))) {
			StanceApplier.removeStanceModifiers(player);
			}
			runtime.clear(oldDef);
		}
		runtime.resetCooldowns();
	}

	/**
	 * Spender consume: drops the one passive's stacks and timestamp and takes the armor they
	 * earned off the player. Reports whether anything was consumed. Nothing held means
	 * nothing done. Only the priced passive is touched: a second stacker on the class
	 * keeps every stack.
	 */
	private static boolean consumeStacks(ServerPlayer player, PlayerRuntime runtime, ConditionalPassive passive) {
		if (passive == null || runtime.stacks(passive) <= 0) {
			return false;
		}
		runtime.clear(passive);
		AttributeWriter.remove(player, passive.attribute(), passive.modifierId());
		return true;
	}

	/**
	 * Spender consume by class: resolves the priced passive by trigger and consumes it. The
	 * single path for Rend, so the entry never looks the passive up itself: the definition
	 * owns the one trigger scan, and the entry stays out.
	 */
	public static boolean consumeSpent(ServerPlayer player, PlayerRuntime runtime, ClassDefinition def) {
		return consumeStacks(player, runtime,
				def == null ? null : def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT));
	}

	/**
	 * Bonus grant by class: Rush's next qualifying melee hit credits the priced passive
	 * with the ability's bonus, capped and grace re-stamped through the trigger path.
	 * Resolves through the definition like {@link #consumeSpent}: the entry forwards,
	 * never resolves. Only a live fired window with its bonus still pending
	 * grants; rejects, stale windows and a missing priced passive do nothing.
	 */
	public static void grantRushBonus(PlayerRuntime runtime, ClassDefinition def, long now) {
		ConditionalPassive momentum =
				def == null ? null : def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		if (momentum == null || !runtime.consumeRushBonus(now)) {
			return;
		}
		for (int i = 0; i < RushAbility.BONUS_STACKS; i++) {
			runtime.onTrigger(def, momentum.trigger(), now);
		}
	}
}
