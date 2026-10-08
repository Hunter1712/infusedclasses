package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * Stance applier: the adapter that makes the player's attributes agree with the
 * Legionary stance. Only the current stance's modifiers sit on the player, the other
 * stance's come off, same write-only-on-change shape as the Vigor tick. Testudo plate
 * and brace ride the window while live. Non-Legionaries skip entirely; leaving the
 * class removes everything through {@link #removeStanceModifiers}.
 */
final class StanceApplier {
	private StanceApplier() {
	}

	/**
	 * Legionary stance auras: only the current stance's modifiers sit on the player,
	 * the other stance's come off. Non-Legionaries skip entirely.
	 */
	static void applyStance(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		if (!def.activeAbilities().contains(ClassRegistry.ROTATE_ABILITY)) {
			return;
		}
		if (runtime.stance() == RotateAbility.Stance.GLADIUS) {
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.ATTACK_DAMAGE, RotateAbility.GLADIUS_DAMAGE_ID, RotateAbility.GLADIUS_DAMAGE));
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.ATTACK_SPEED, RotateAbility.GLADIUS_ALACRITY_ID, RotateAbility.GLADIUS_SPEED));
			AttributeWriter.remove(player, ClassRegistry.ARMOR, RotateAbility.SCUTUM_PLATE_ID);
			AttributeWriter.remove(player, ClassRegistry.ARMOR_TOUGHNESS, RotateAbility.SCUTUM_TOUGHNESS_ID);
			AttributeWriter.remove(player, ClassRegistry.MOVEMENT_SPEED, RotateAbility.SCUTUM_WEIGHT_ID);
		} else {
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.ARMOR, RotateAbility.SCUTUM_PLATE_ID, RotateAbility.SCUTUM_ARMOR));
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.ARMOR_TOUGHNESS, RotateAbility.SCUTUM_TOUGHNESS_ID, RotateAbility.SCUTUM_TOUGHNESS));
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.MOVEMENT_SPEED, RotateAbility.SCUTUM_WEIGHT_ID, RotateAbility.SCUTUM_WEIGHT));
			AttributeWriter.remove(player, ClassRegistry.ATTACK_DAMAGE, RotateAbility.GLADIUS_DAMAGE_ID);
			AttributeWriter.remove(player, ClassRegistry.ATTACK_SPEED, RotateAbility.GLADIUS_ALACRITY_ID);
		}
		if (runtime.isTestudoLive(now)) {
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.ARMOR, TestudoAbility.TESTUDO_PLATE_ID, TestudoAbility.TESTUDO_ARMOR));
			AttributeWriter.write(player, new PassiveEffect.Modifier(
					ClassRegistry.KNOCKBACK_RESISTANCE, TestudoAbility.TESTUDO_BRACE_ID, 1.0));
		} else {
			AttributeWriter.remove(player, ClassRegistry.ARMOR, TestudoAbility.TESTUDO_PLATE_ID);
			AttributeWriter.remove(player, ClassRegistry.KNOCKBACK_RESISTANCE, TestudoAbility.TESTUDO_BRACE_ID);
		}
	}

	/** Every stance/window modifier in one place: leaving Legionary takes them all off. */
	static void removeStanceModifiers(ServerPlayer player) {
		AttributeWriter.remove(player, ClassRegistry.ATTACK_DAMAGE, RotateAbility.GLADIUS_DAMAGE_ID);
		AttributeWriter.remove(player, ClassRegistry.ATTACK_SPEED, RotateAbility.GLADIUS_ALACRITY_ID);
		AttributeWriter.remove(player, ClassRegistry.ARMOR, RotateAbility.SCUTUM_PLATE_ID);
		AttributeWriter.remove(player, ClassRegistry.ARMOR_TOUGHNESS, RotateAbility.SCUTUM_TOUGHNESS_ID);
		AttributeWriter.remove(player, ClassRegistry.MOVEMENT_SPEED, RotateAbility.SCUTUM_WEIGHT_ID);
		AttributeWriter.remove(player, ClassRegistry.ARMOR, TestudoAbility.TESTUDO_PLATE_ID);
		AttributeWriter.remove(player, ClassRegistry.KNOCKBACK_RESISTANCE, TestudoAbility.TESTUDO_BRACE_ID);
	}
}
