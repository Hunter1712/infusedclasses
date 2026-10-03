package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Passive attribute tweak. Plain data, no game bootstrap needed, so tests assert on it directly.
 */
public record Passive(Identifier attribute, Identifier modifier, double value, AttributeModifier.Operation operation) {
}

