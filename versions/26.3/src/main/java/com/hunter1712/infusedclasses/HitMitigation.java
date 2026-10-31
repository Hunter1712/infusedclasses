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
		MobEffectInstance resistance = living.getEffect(MobEffects.RESISTANCE);
		int amplifier = resistance == null ? -1 : resistance.getAmplifier();
		float protection = 0.0F;
		if (landed > 0.0F && !source.is(DamageTypeTags.BYPASSES_EFFECTS)
				&& !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
				&& living.level() instanceof ServerLevel level) {
			protection = EnchantmentHelper.getDamageProtection(level, living, source);
		}
		return landedAfterArmor(landed, amplifier, protection,
				source.is(DamageTypeTags.BYPASSES_EFFECTS),
				source.is(DamageTypeTags.BYPASSES_RESISTANCE),
				source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS));
	}

	/**
	 * The pure layer after vanilla armor: Resistance then protection, with each bypass
	 * tag skipping its own layer. A negative amplifier is no Resistance effect. Vanilla
	 * owns the armor formula because its armor-effectiveness enchantment needs the live
	 * entity and source, so this seam starts at the already-armored amount.
	 */
	static float landedAfterArmor(float landed, int resistanceAmplifier, float protection,
			boolean bypassEffects, boolean bypassResistance, boolean bypassEnchantments) {
		if (bypassEffects) {
			return Math.max(0.0F, landed);
		}
		if (landed > 0.0F && !bypassResistance && resistanceAmplifier >= 0) {
			int percent = (resistanceAmplifier + 1) * 5;
			landed = Math.max(0.0F, landed * (25 - percent) / 25.0F);
		}
		if (landed > 0.0F && !bypassEnchantments && protection > 0.0F) {
			landed = CombatRules.getDamageAfterMagicAbsorb(landed, protection);
		}
		return landed;
	}
}
