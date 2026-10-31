package com.hunter1712.infusedclasses;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Attribute writer mechanics: the single place modifiers are written and removed.
 * Writes only what the decision names, and only when it differs from what is applied,
 * so a modifier is created once and left alone while the stacks hold still.
 *
 * <p>1.20.1 keys a modifier by {@link UUID}; the decision's id string hashes to one
 * deterministically, so the same id always maps to the same modifier.
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
		UUID id = uuid(wanted.modifierId());
		AttributeModifier existing = instance.getModifier(id);
		if (existing != null) {
			if (existing.getAmount() == wanted.amount()) {
				return; // already correct
			}
			instance.removeModifier(id);
		}
		instance.addPermanentModifier(new AttributeModifier(
				id, wanted.modifierId().toString(), wanted.amount(), AttributeModifier.Operation.ADDITION));
	}

	/** Takes off a modifier stacks used to earn. A no-op when there is none. */
	static void remove(ServerPlayer player, ResourceKey<Attribute> attribute, ResourceLocation modifierId) {
		AttributeInstance instance = attribute(player, attribute);
		if (instance == null || instance.getModifier(uuid(modifierId)) == null) {
			return;
		}
		instance.removeModifier(uuid(modifierId));
	}

	private static AttributeInstance attribute(ServerPlayer player, ResourceKey<Attribute> key) {
		Registry<Attribute> registry = player.serverLevel().registryAccess().registryOrThrow(Registries.ATTRIBUTE);
		Attribute attribute = registry.get(key);
		return attribute == null ? null : player.getAttribute(attribute);
	}

	private static UUID uuid(ResourceLocation id) {
		return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
	}
}
