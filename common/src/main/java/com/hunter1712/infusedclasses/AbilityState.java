package com.hunter1712.infusedclasses;

/**
 * Transient stance, flip-record and Streak state: one small read per status path.
 * The Legionary's stance, the last Rotate flip's landing, the live Roulette band and the
 * Gambler's Streak counter all die with the session. No clock, no player, no choice —
 * time and the class arrive as parameters where they are read.
 */
final class AbilityState {
	private RotateAbility.Stance stance = RotateAbility.Stance.SCUTUM;
	private RotateAbility.Stance flippedTo = null;
	private RouletteAbility.Outcome rouletteOutcome = null;
	private int streak = 0;

	RotateAbility.Stance stance() {
		return stance;
	}

	void stance(RotateAbility.Stance stance) {
		this.stance = stance;
	}

	RotateAbility.Stance flippedTo() {
		return flippedTo;
	}

	void flippedTo(RotateAbility.Stance flippedTo) {
		this.flippedTo = flippedTo;
	}

	RouletteAbility.Outcome rouletteOutcome() {
		return rouletteOutcome;
	}

	void rouletteOutcome(RouletteAbility.Outcome rouletteOutcome) {
		this.rouletteOutcome = rouletteOutcome;
	}

	int streak() {
		return streak;
	}

	void bumpStreak() {
		streak++;
	}

	void resetStreak() {
		streak = 0;
	}

	void resetAll() {
		stance = RotateAbility.Stance.SCUTUM;
		flippedTo = null;
		rouletteOutcome = null;
		streak = 0;
	}
}
