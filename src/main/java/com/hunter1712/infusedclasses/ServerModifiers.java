package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Production adapter: resolves attributes against live holders for one server player. */
public final class ServerModifiers implements ModifierSink {
	private final ServerPlayer player;

	public ServerModifiers(ServerPlayer player) {
		this.player = player;
	}

	@Override
	public void removeModifier(Identifier attribute, Identifier modifier) {
		AttributeInstance inst = instance(attribute);
		if (inst != null) {
			inst.removeModifier(modifier);
		}
	}

	@Override
	public void addModifier(Identifier attribute, Identifier modifier, double value,
			AttributeModifier.Operation operation) {
		AttributeInstance inst = instance(attribute);
		if (inst == null) {
			return;
		}
		inst.removeModifier(modifier);
		inst.addPermanentModifier(new AttributeModifier(modifier, value, operation));
	}

	private AttributeInstance instance(Identifier attribute) {
		Holder<Attribute> holder = ClassAttributes.resolve(attribute);
		return holder == null ? null : player.getAttribute(holder);
	}
}
