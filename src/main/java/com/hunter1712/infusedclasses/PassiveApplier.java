package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

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
				case PassiveEffect.Plan.Write(var wanted) -> write(player, wanted);
				case PassiveEffect.Plan.Remove(var attribute, var modifierId) -> remove(player, attribute, modifierId);
				case PassiveEffect.Plan.Skip() -> {
				}
			}
		}
		// Berserk: no stacks, no grace. Recomputed from live HP every tick; the write
		// early-out below already covers no-op ticks, bucket 0 removes like dead stacks.
		int bucket = HpScaledPassive.bucket(player.getHealth(), player.getMaxHealth());
		for (HpScaledPassive hp : def.hpPassives()) {
			PassiveEffect.Modifier wanted = PassiveEffect.forBucket(hp, bucket);
			if (wanted != null) {
				write(player, wanted);
			} else {
				remove(player, hp.attribute(), hp.modifierId());
			}
		}
		// Bonus pool: flat max-HP per class, Warrior-only today. The write early-out
		// covers no-op ticks; a zero bonus removes, so the tick self-heals within one
		// tick of any switch — no onClassChanged handling needed.
		if (def.maxHealthBonus() > 0) {
			write(player, new PassiveEffect.Modifier(
					ClassRegistry.MAX_HEALTH, ClassRegistry.BONUS_HEALTH_ID, def.maxHealthBonus()));
		} else {
			remove(player, ClassRegistry.MAX_HEALTH, ClassRegistry.BONUS_HEALTH_ID);
		}
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
					remove(player, passive.attribute(), passive.modifierId());
				}
			}
			// Berserk holds no stacks to clear, only the modifiers come off.
			for (HpScaledPassive hp : oldDef.hpPassives()) {
				if (newDef == null || !newDef.hpPassives().contains(hp)) {
					remove(player, hp.attribute(), hp.modifierId());
				}
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
	public static boolean consumeStacks(ServerPlayer player, PlayerRuntime runtime, ConditionalPassive passive) {
		if (passive == null || runtime.stacks(passive) <= 0) {
			return false;
		}
		runtime.clear(passive);
		remove(player, passive.attribute(), passive.modifierId());
		return true;
	}

	/**
	 * Spender consume by class: resolves the priced passive by trigger and consumes it. The
	 * single path for Second Wind, so the entry never looks the passive up itself: one
	 * trigger scan in the decision, one here, and never one in the entry.
	 */
	public static boolean consumeSpent(ServerPlayer player, PlayerRuntime runtime, ClassDefinition def) {
		return consumeStacks(player, runtime, SecondWindAbility.bulwarkOf(def));
	}

	/**
	 * Builder grant by class: War Cry's shout credits the priced passive, capped and
	 * grace re-stamped through the trigger path. The single path for the builder, mirroring
	 * {@link #consumeSpent}: the entry forwards the result, never resolves. Only a fired
	 * shout grants; rejects and a missing priced passive do nothing.
	 */
	public static void grantWarCryStacks(PlayerRuntime runtime, ClassDefinition def,
			WarCryAbility.Result result, long now) {
		if (result == null || result.kind() != WarCryAbility.Kind.FIRED) {
			return;
		}
		ConditionalPassive bulwark = SecondWindAbility.bulwarkOf(def);
		if (bulwark == null) {
			return;
		}
		for (int i = 0; i < WarCryAbility.BUILDER_STACKS; i++) {
			runtime.onTrigger(def, bulwark.trigger(), now);
		}
	}

	/** Writes exactly what the decision names, and only when it differs from what is applied. */
	private static void write(ServerPlayer player, PassiveEffect.Modifier wanted) {
		AttributeInstance instance = attribute(player, wanted.attribute());
		if (instance == null) {
			return;
		}
		AttributeModifier existing = instance.getModifier(wanted.modifierId());
		if (existing != null) {
			if (existing.amount() == wanted.amount()) {
				return; // already correct
			}
			instance.removeModifier(wanted.modifierId());
		}
		instance.addPermanentModifier(new AttributeModifier(
				wanted.modifierId(), wanted.amount(), AttributeModifier.Operation.ADD_VALUE));
	}

	/** Takes off a modifier stacks used to earn. A no-op when there is none. */
	private static void remove(ServerPlayer player, ResourceKey<Attribute> attribute, Identifier modifierId) {
		AttributeInstance instance = attribute(player, attribute);
		if (instance != null) {
			instance.removeModifier(modifierId);
		}
	}

	private static AttributeInstance attribute(ServerPlayer player, ResourceKey<Attribute> key) {
		Holder<Attribute> holder = player.registryAccess().lookupOrThrow(Registries.ATTRIBUTE)
				.get(key)
				.orElse(null);
		return holder == null ? null : player.getAttribute(holder);
	}
}
