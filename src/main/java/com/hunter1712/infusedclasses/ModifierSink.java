package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Live modifier seam. Production serves it with a server player behind it;
 * tests serve it with an in-memory recording adapter. Pure strip/apply order
 * and unknown-attribute skipping are decided before touching the sink.
 */
public interface ModifierSink {
	void removeModifier(Identifier attribute, Identifier modifier);

	void addModifier(Identifier attribute, Identifier modifier, double value, AttributeModifier.Operation operation);
}
