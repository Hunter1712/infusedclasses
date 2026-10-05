package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Adapter for {@link PassiveEffect}: makes the player's attributes agree with the decision. */
public final class PassiveApplier {
	private PassiveApplier() {
	}

	/**
	 * Idempotent: an attribute is only written when what is applied differs from what should be,
	 * so the modifier is created once and left alone while the stacks hold still.
	 */
	public static void apply(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime,
			boolean stacksMoved) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			PassiveEffect.Modifier wanted = PassiveEffect.forStacks(passive, runtime.stacks(passive));
			if (wanted == null) {
				// No decision means no stacks. The only thing that can still be on the player is
				// a modifier those stacks earned, and only the definition knows the id it was
				// written under, so this is the one path that reads the passive rather than the
				// decision. There is no decision to be inconsistent with here.
				if (stacksMoved) {
					remove(player, passive.attribute(), passive.modifierId());
				}
				continue;
			}
			write(player, wanted);
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
