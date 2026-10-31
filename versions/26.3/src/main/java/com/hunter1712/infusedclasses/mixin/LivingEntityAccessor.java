package com.hunter1712.infusedclasses.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The one vanilla field the settle adapter must nudge: {@code lastHurt} remembers the
 * biggest hurt of the current invulnerability window, and the damage path drops any
 * same-tick follow-up that does not beat it. Exposed so {@code DamageSettle} can clear
 * it around a bonus hurt and put it back after; it holds zero policy.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("lastHurt")
	float infused$lastHurt();

	@Accessor("lastHurt")
	void infused$lastHurt(float value);
}
