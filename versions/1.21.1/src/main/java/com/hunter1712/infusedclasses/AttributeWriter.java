package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Attribute writer mechanics: the single place modifiers are written and removed.
 * Writes only what the decision names, and only when it differs from what is applied,
 * so a modifier is created once and left alone while the stacks hold still.
 *
 * <p>Two seams where plain data becomes game objects: {@link #modifierId} turns the
 * decision's id string into the key a modifier is written under, and {@link #key}
 * turns the decision's {@link AttributeRef} into a registry key.
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
		ResourceLocation id = modifierId(wanted.modifierId());
		AttributeModifier existing = instance.getModifier(id);
		if (existing != null) {
			if (existing.amount() == wanted.amount()) {
				return; // already correct
			}
			instance.removeModifier(id);
		}
		instance.addPermanentModifier(new AttributeModifier(
				id, wanted.amount(), AttributeModifier.Operation.ADD_VALUE));
	}

	/** Takes off a modifier stacks used to earn. A no-op when there is none. */
	static void remove(ServerPlayer player, AttributeRef attribute, String modifierId) {
		AttributeInstance instance = attribute(player, attribute);
		ResourceLocation id = modifierId(modifierId);
		if (instance == null || instance.getModifier(id) == null) {
			return;
		}
		instance.removeModifier(id);
	}

	private static ResourceLocation modifierId(String id) {
		return ResourceLocation.parse(id);
	}

	private static AttributeInstance attribute(ServerPlayer player, AttributeRef ref) {
		Holder<Attribute> holder = player.registryAccess().lookupOrThrow(Registries.ATTRIBUTE)
				.get(key(ref))
				.orElse(null);
		return holder == null ? null : player.getAttribute(holder);
	}

	private static ResourceKey<Attribute> key(AttributeRef ref) {
		return ResourceKey.create(Registries.ATTRIBUTE, modifierId(ref.id()));
	}
}
