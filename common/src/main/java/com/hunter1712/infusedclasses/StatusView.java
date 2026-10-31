package com.hunter1712.infusedclasses;

/**
 * The three reads a status rider makes: one lane's read, the live flip window's target,
 * and the wheel's band. Defined here, as the seam the decision side needs, so a rider
 * rule can be asserted without a packet on the wire; the wire snapshot implements it in
 * the version module. Nothing else about the snapshot reaches the decision.
 */
public interface StatusView {
	/** One lane's whole read: its remainder and window state. */
	LaneRead lane(Lane lane);

	/** The live flip window's landing, null when no flip window is live. */
	RotateAbility.Stance flipTarget();

	/** The wheel's band, null when the wheel never spun this lane's life. */
	RouletteAbility.Outcome rouletteOutcome();
}
