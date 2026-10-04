package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deep passive module: strip-then-apply order plus unknown-attribute skipping
 * behind one small interface. Callers and tests cross the same ModifierSink seam.
 */
public final class ClassApplier {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassApplier.class);

	private ClassApplier() {
	}

	public static void reapply(ModifierSink sink, ClassDefinition def) {
		stripAll(sink);
		applyAll(sink, def);
	}

	/** Pure strip: every pair this mod ever adds, through the sink. */
	public static void stripAll(ModifierSink sink) {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (Passive p : def.passives()) {
				sink.removeModifier(p.attribute(), p.modifier());
			}
		}
	}

	/** Pure apply: base definition, unknown attributes skipped with a warning. */
	public static void applyAll(ModifierSink sink, ClassDefinition def) {
		for (Passive p : def.passives()) {
			applyOne(sink, def.id(), p);
		}
	}

	private static void applyOne(ModifierSink sink, Identifier classId, Passive p) {
		if (!ClassAttributes.isKnown(p.attribute())) {
			LOGGER.warn("Unknown attribute {} for class {}, skipping passive", p.attribute(), classId);
			return;
		}
		sink.removeModifier(p.attribute(), p.modifier());
		sink.addModifier(p.attribute(), p.modifier(), p.value(), p.operation());
	}
}
