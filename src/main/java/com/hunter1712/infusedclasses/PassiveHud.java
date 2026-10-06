package com.hunter1712.infusedclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Pure HUD-row decision for conditional passives. Given a passive, its stacks and the
 * millis until decay starts, it answers what one effect-like row shows: icon id, title,
 * subline, seconds-left, bar fraction and the hidden flag. No DrawContext, player or
 * clock inside; adapters draw whatever this returns.
 */
public final class PassiveHud {
	/** Effects-group icon only, never a loaded texture; the adapter resolves it. */
	public static final String ICON = "minecraft:resistance";
	/** War Cry row icon: Strength II is implied, so the title carries no level. */
	public static final String WAR_CRY_ICON = "minecraft:strength";

	private PassiveHud() {
	}

	/** One effect-like row, as the adapter draws it verbatim. */
	public record Row(String iconId, String title, String subline, int secondsLeft, float barFraction, boolean hidden) {
	}

	/** Stacks N reads as level N: 3 stacks is Bulwark III. Past the cap, the bare number. */
	public static String roman(int stacks) {
		return switch (stacks) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			case 9 -> "IX";
			case 10 -> "X";
			default -> Integer.toString(stacks);
		};
	}

	/**
	 * One row for one passive. The countdown is the grace left, vanilla-style: each hit
	 * resets it to the full grace, and past grace every stack is already gone. The bar
	 * drains the same grace as its warning.
	 */
	public static Row rowFor(ConditionalPassive passive, int stacks, long millisUntilDecay) {
		if (passive == null || stacks <= 0) {
			return new Row(ICON, "", "", 0, 0f, true);
		}
		double left = Math.min(passive.graceSeconds(), Math.max(0, millisUntilDecay) / 1000.0);
		float fraction = passive.graceSeconds() <= 0 ? 0f : (float) (left / passive.graceSeconds());
		return new Row(ICON, passive.name() + " " + roman(stacks),
				"+" + amount(stacks * passive.perStack()) + " " + word(passive),
				(int) Math.ceil(left), fraction, false);
	}

	/**
	 * One row for War Cry's Strength window: title without the level, seconds-left from the
	 * signalled ticks, and the bar full at a fresh 8s window so extended upkeep reads as full.
	 * Hidden at zero or fewer ticks; beacon Strength never feeds this, only fire and proc events do.
	 */
	public static Row warCryRow(int ticksLeft) {
		if (ticksLeft <= 0) {
			return new Row(WAR_CRY_ICON, "", "", 0, 0f, true);
		}
		float fraction = Math.min(1f, ticksLeft / (float) WarCryAbility.EFFECT_TICKS);
		return new Row(WAR_CRY_ICON, "War Cry", "Strength", (ticksLeft + 19) / 20, fraction, false);
	}

	/** Only stacks above zero, in the order the class carries them. */
	public static List<ConditionalPassive> visiblePassives(ClassDefinition def, ToIntFunction<ConditionalPassive> stacksOf) {
		if (def == null) {
			return List.of();
		}
		List<ConditionalPassive> out = new ArrayList<>();
		for (ConditionalPassive passive : def.passives()) {
			if (stacksOf.applyAsInt(passive) > 0) {
				out.add(passive);
			}
		}
		return List.copyOf(out);
	}

	/** Only stacks above zero, one row per passive in class-registry order. */
	public static List<ConditionalPassive> visibleInRegistryOrder(ToIntFunction<ConditionalPassive> stacksOf) {
		List<ConditionalPassive> out = new ArrayList<>();
		for (ClassDefinition def : ClassRegistry.all()) {
			for (ConditionalPassive passive : visiblePassives(def, stacksOf)) {
				if (!out.contains(passive)) {
					out.add(passive);
				}
			}
		}
		return List.copyOf(out);
	}

	/** Attribute name as rows spell it: "minecraft:armor" reads as "armor". */
	private static String word(ConditionalPassive passive) {
		return passive.attribute().identifier().getPath().replace('_', ' ');
	}

	/** Whole numbers read as "1", not "1.0", like the picker copy. */
	private static String amount(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
	}
}
