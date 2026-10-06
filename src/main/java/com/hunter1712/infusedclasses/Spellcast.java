package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

/**
 * Casting commitment shared by every active: a short heavy self-slow, identical on all
 * casts, so "casting roots you" is one learnable rule. Decision data (an id plus numbers),
 * applied through the shared adapter helper; picker copy is built from these constants.
 */
public final class Spellcast {
	public static final Identifier SLOWNESS = Identifier.withDefaultNamespace("slowness");
	/** Root window in ticks: 1 second, inside the shout's 8s and gone long before any lane. */
	public static final int SLOW_TICKS = 20;
	/** Amplifier for the root: Slowness III — planted feet, still mobile. */
	public static final int SLOW_AMPLIFIER = 2;

	private Spellcast() {
	}
}
