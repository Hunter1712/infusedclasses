package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Stance applier: the adapter that makes the player's attributes agree with the
 * Legionary stance. Only the current stance's modifiers sit on the player, the other
 * stance's come off, same write-only-on-change shape as the Vigor tick. Aequilibrium edge
 * and plate ride the window while live. Non-Legionaries skip entirely; leaving the
 * class removes everything through {@link #onClassChanged}.
 */
final class StanceApplier {
	private StanceApplier() {
	}

	/**
	 * Legionary stance auras: only the current stance's modifiers sit on the player,
	 * the other stance's come off. Non-Legionaries skip entirely. Both sides iterate
	 * the stance's single aura list, so write and remove cannot drift.
	 */
	static void applyStance(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		if (!def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return;
		}
		for (PassiveEffect.Modifier wanted : RotateAbility.auraFor(runtime.stance())) {
			AttributeWriter.write(player, wanted);
		}
		for (PassiveEffect.Modifier stale : RotateAbility.auraFor(RotateAbility.flipped(runtime.stance()))) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
		if (runtime.isAequilibriumLive(now)) {
			for (PassiveEffect.Modifier wanted : AequilibriumAbility.windowModifiers()) {
				AttributeWriter.write(player, wanted);
			}
		} else {
			for (PassiveEffect.Modifier stale : AequilibriumAbility.windowModifiers()) {
				AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
			}
		}
	}

	/** Every stance/window modifier in one place: leaving Legionary takes them all off. */
	static void removeStanceModifiers(ServerPlayer player) {
		List<PassiveEffect.Modifier> all = new ArrayList<>(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS));
		all.addAll(RotateAbility.auraFor(RotateAbility.Stance.SCUTUM));
		all.addAll(AequilibriumAbility.windowModifiers());
		for (PassiveEffect.Modifier stale : all) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
	}

	/**
	 * Class switch: leaving Legionary takes the stance auras off. The marker lives
	 * here with the auras, so the passive adapter never checks it. Staying Legionary
	 * (or never having been one) touches nothing: the tick owns the steady state.
	 */
	static void onClassChanged(ServerPlayer player, ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef != null && oldDef.hasAbility(ClassRegistry.ROTATE_ABILITY)
				&& (newDef == null || !newDef.hasAbility(ClassRegistry.ROTATE_ABILITY))) {
			removeStanceModifiers(player);
		}
	}
}
