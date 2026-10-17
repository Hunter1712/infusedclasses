package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The class-switch adapter: the single path behind every switch entry. Validates the
 * pick in the choice module, cleans what the old class earned, and announces the
 * outcome; callers keep only their own extra (the confirm packet, the command code).
 */
public final class ClassSwitch {
	private ClassSwitch() {
	}

	/**
	 * Switches the player to the given class id and announces the outcome, rejection
	 * included. Returns the new definition, or null when the pick was rejected.
	 */
	public static ClassDefinition trySwitch(ServerPlayer player, Identifier id) {
		PlayerClasses classes = PlayerClassAccess.of(player);
		ClassDefinition oldDef = classes.currentDefinition();
		ClassDefinition def = classes.trySetClass(id);
		if (def == null) {
			AbilityApplier.announce(player, ChatLine.unknownOrLocked(), false);
			return null;
		}
		PlayerRuntime runtime = InfusedClasses.runtime(player);
		PassiveApplier.onClassChanged(player, runtime, oldDef, def);
		AbilityApplier.announce(player, ChatLine.classPicked(def.displayName()), false);
		// One emission seam for both switch entries: the picker receiver and /class set
		// tell the client what the stack shows now, and a rejected pick sent nothing.
		StatusSync.maybeSend(player, System.currentTimeMillis());
		return def;
	}
}
