package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Adapter for {@link PassiveEffect}: makes the player's attributes agree with the decision. */
public final class PassiveApplier {
	private PassiveApplier() {
	}

	/**
	 * One tick for one player: the single reconcile home. Bleed, then the tiered bars,
	 * then the always-on chassis, then the window-aura families
	 * (the stance, Aequilibrium's oath window, the wheel's live spin, Offload's loosened
	 * window), in that order; the switch's cleanup takes off what {@link #cleanupFor}
	 * decides is stale. The tick order and the write-only-on-change discipline are
	 * decided once. The caller owns the tick's one state read and passes it: the bleed
	 * touches no window or lane, so the read is the same before or after it. Idempotent:
	 * an attribute is only written when what is applied differs from what should be, so
	 * the modifier is created once and left alone while the bar holds still.
	 */
	public static void apply(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime,
			PlayerRuntime.State state, long now) {
		if (def == null) {
			return;
		}
		runtime.decay(def, now);
		for (ConditionalPassive passive : def.passives()) {
			// The bar owns its rows: each passive names what it writes at a stack count,
			// so a second bar adds its own row, not a dispatch here.
			reconcile(player, passive.allRows(),
					passive.barRows().apply(runtime.stacks(passive)));
		}
		for (ClassDefinition.AlwaysOn alwaysOn : def.alwaysOn()) {
			for (PassiveEffect.Modifier modifier : alwaysOn.modifiers()) {
				AttributeWriter.write(player, modifier);
			}
		}
		reconcileWindows(player, def, state);
	}

	/** One reconcile pass over every family: a held marker writes what its state wants, an absent one wants nothing. */
	private static void reconcileWindows(ServerPlayer player, ClassDefinition def, PlayerRuntime.State state) {
		for (WindowFamily family : WindowFamily.allFamilies()) {
			List<PassiveEffect.Modifier> wanted = def.hasAbility(family.marker())
					? family.wanted().apply(state) : List.of();
			reconcile(player, family.all(), wanted);
		}
	}

	/**
	 * The one reconcile pass: write every wanted modifier, then take off every sweep id
	 * the wanted set does not name. Keyed by id, so two rows sharing an id write once and
	 * a row that changed its amount overwrites in place instead of remove-then-write.
	 */
	private static void reconcile(ServerPlayer player, List<PassiveEffect.Modifier> all,
			List<PassiveEffect.Modifier> wanted) {
		for (PassiveEffect.Modifier modifier : wanted) {
			AttributeWriter.write(player, modifier);
		}
		for (PassiveEffect.Modifier modifier : all) {
			if (!names(wanted, modifier.modifierId())) {
				AttributeWriter.remove(player, modifier.attribute(), modifier.modifierId());
			}
		}
	}

	/** Whether any modifier in the list carries the id. */
	private static boolean names(List<PassiveEffect.Modifier> modifiers, String id) {
		for (PassiveEffect.Modifier modifier : modifiers) {
			if (modifier.modifierId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Class switch: the new class's tick never visits the old passives, so their modifiers
	 * would orphan. Removes every modifier {@link #cleanupFor} says is stale, drops the old
	 * stacks, and resets the runtime: a new class starts empty. The rule is a decision, so
	 * what a switch takes off is assertable without a player.
	 */
	public static void onClassChanged(ServerPlayer player, PlayerRuntime runtime,
			ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef == newDef) {
			return;
		}
		for (PassiveEffect.Modifier stale : cleanupFor(oldDef, newDef)) {
			AttributeWriter.remove(player, stale.attribute(), stale.modifierId());
		}
		runtime.clear(oldDef);
		runtime.resetAll();
	}

	/** What a switch takes off. {@link PassiveSweep#sweepFor} is the decision; this only removes. */
	static List<PassiveEffect.Modifier> cleanupFor(ClassDefinition oldDef, ClassDefinition newDef) {
		return PassiveSweep.sweepFor(oldDef, newDef);
	}
}
