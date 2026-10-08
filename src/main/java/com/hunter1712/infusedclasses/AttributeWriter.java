package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Attribute writer mechanics: the single place modifiers are written and removed.
 * Writes only what the decision names, and only when it differs from what is applied,
 * so a modifier is created once and left alone while the stacks hold still.
 */
final class AttributeWriter {
	private AttributeWriter() {
	}

	/** Writes exactly what the decision names, and only when it differs from what is applied. */
	static void write(ServerPlayer player, PassiveEffect.Modifier wanted) {
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
	static void remove(ServerPlayer player, ResourceKey<Attribute> attribute, Identifier modifierId) {
		AttributeInstance instance = attribute(player, attribute);
		if (instance != null) {
			instance.removeModifier(modifierId);
		}
	}

	static AttributeInstance attribute(ServerPlayer player, ResourceKey<Attribute> key) {
		Holder<Attribute> holder = player.registryAccess().lookupOrThrow(Registries.ATTRIBUTE)
				.get(key)
				.orElse(null);
		return holder == null ? null : player.getAttribute(holder);
	}
}
