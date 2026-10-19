package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * What a hit lands on a victim: the amount the damage stays at after armor, resistance
 * and enchantments, in the same order and with the same tag gates as
 * {@code LivingEntity.getDamageAfterArmorAbsorb} + {@code getDamageAfterMagicAbsorb},
 * using their own {@code CombatRules} helpers. The instance pair itself is not called:
 * its armor-durability hit and resistance stats belong to the real hit once, and the
 * settle needs the number, not the side effects. Consult those methods when vanilla
 * changes. Pure reads and arithmetic: no writes, no clock.
 */
final class HitMitigation {
	private HitMitigation() {
	}

	/** The hit's post-mitigation amount on this victim, immunity tags included. */
	static float landedOn(LivingEntity living, DamageSource source, float damageTaken) {
		float landed = damageTaken;
		if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
			landed = CombatRules.getDamageAfterAbsorb(living, damageTaken, source, living.getArmorValue(),
					(float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		}
		if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
			return Math.max(0.0F, landed);
		}
		MobEffectInstance resistance = living.getEffect(MobEffects.RESISTANCE);
		if (!source.is(DamageTypeTags.BYPASSES_RESISTANCE) && resistance != null) {
			int amplifier = (resistance.getAmplifier() + 1) * 5;
			landed = Math.max(0.0F, landed * (25 - amplifier) / 25.0F);
		}
		if (landed > 0.0F && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
				&& living.level() instanceof ServerLevel level) {
			float protection = EnchantmentHelper.getDamageProtection(level, living, source);
			if (protection > 0.0F) {
				landed = CombatRules.getDamageAfterMagicAbsorb(landed, protection);
			}
		}
		return landed;
	}
}
