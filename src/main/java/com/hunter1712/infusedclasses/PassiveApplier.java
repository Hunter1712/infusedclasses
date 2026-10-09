package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Adapter for {@link PassiveEffect}: makes the player's attributes agree with the decision. */
public final class PassiveApplier {
	private PassiveApplier() {
	}

	/**
	 * One tick for one player: the single reconcile home. Decay, then conditionals, then
	 * the HP-scaled pair, then the stance aura, in that order; the switch cleanup mirrors
	 * it in {@link #onClassChanged}. Stance rides here (not in the entry tick) so the tick
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
		// ponytail: no max-health tick until a class carries one (all read 0.0 today).
		// A class that sets maxHealthBonus re-adds one plan computation here; nothing else writes it.
		StanceApplier.applyStance(player, def, runtime, now);
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
	 * The one priced passive on a class: the first {@code on_hit_dealt} stacker, or null.
	 * Resolved once in {@link RendAbility#spentPassive}: the spender pair (consume plus
	 * Rush grant) and Rend's decide path share it, so neither resolves behind the other's back.
	 */
	private static ConditionalPassive pricedPassive(ClassDefinition def) {
		return RendAbility.spentPassive(def);
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
			// The Legionary marker lives with the stance adapter, not here.
			StanceApplier.onClassChanged(player, oldDef, newDef);
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
		return consumeStacks(player, runtime, pricedPassive(def));
	}

	/**
	 * Bonus grant by class: Rush's next qualifying melee hit credits the priced passive
	 * with the ability's bonus, capped and grace re-stamped in one call.
	 * Resolves through the definition like {@link #consumeSpent}: the entry forwards,
	 * never resolves. Only a live fired window with its bonus still pending
	 * grants; rejects, stale windows and a missing priced passive do nothing.
	 */
	public static void grantRushBonus(PlayerRuntime runtime, ClassDefinition def, long now) {
		ConditionalPassive momentum = pricedPassive(def);
		if (momentum == null || !runtime.consumeRushBonus(now)) {
			return;
		}
		runtime.bumpPassive(momentum, RushAbility.BONUS_STACKS, now);
	}

	/**
	 * Settles a planned hit's one-shot flags at the same {@code now} the plan peeked them,
	 * in field order with {@link MeleePlan#planHit}: the stack bump, the Rush credit, then
	 * the Rend snapshot spend. Returns the stamped Rend bonus for this hit to land, 0 when
	 * the plan saw nothing live: the entry lands what this returns, so it cannot land a
	 * bonus this did not spend, and empty hits never touch a window. Bonus damage itself
	 * still lands from the plan's stamped {@code rendHp}, never recomputed.
	 */
	public static float settleHitOneShots(PlayerRuntime runtime, ClassDefinition def, MeleePlan.HitPlan plan,
			long now) {
		if (runtime == null || plan == null) {
			return 0;
		}
		if (plan.bump()) {
			// plan.bump() already gated on a full swing, so 1.0F replays the same gate.
			runtime.onTrigger(def, ConditionalPassive.ON_HIT_DEALT, now, 1.0F);
		}
		if (plan.rushBonus()) {
			grantRushBonus(runtime, def, now);
		}
		if (plan.rendStacks() > 0 && runtime.consumeRendEmpower(now) >= 0) {
			return plan.rendHp();
		}
		return 0;
	}
}
