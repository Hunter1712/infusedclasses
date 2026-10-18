package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The snapshot decision and its emission guard: builder reads off the transient runtime
 * at a {@code now}, and {@code movedSince} telling discrete movement from quiet decay.
 */
class PlayerStatusTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	@Test
	void builderReadsLanesWindowsAndLoadFlags() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		assertTrue(runtime.arm(Lane.RUSH, now, RushAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.arm(Lane.REND, now, RendAbility.WINDOW_TICKS * 50L, 7));

		PlayerStatusPayload payload = PlayerStatusPayload.of(juggernaut(), runtime, now);

		assertEquals("infusedclasses:juggernaut", payload.classId());
		assertEquals(Lane.RUSH.cooldownMs(), payload.lane(Lane.RUSH).remainingMs());
		assertEquals(Lane.REND.cooldownMs(), payload.lane(Lane.REND).remainingMs());
		assertEquals(0, payload.lane(Lane.ROTATE).remainingMs());
		assertEquals(0, payload.lane(Lane.OATH).remainingMs());
		assertEquals(0, payload.lane(Lane.ROULETTE).remainingMs());
		assertEquals(0, payload.lane(Lane.ALL_IN).remainingMs());
		assertEquals(5_000, payload.lane(Lane.RUSH).windowRemainingMs());
		assertTrue(payload.lane(Lane.RUSH).windowPending());
		assertEquals(6_000, payload.lane(Lane.REND).windowRemainingMs());
		assertTrue(payload.lane(Lane.REND).windowPending());
		assertEquals(0, payload.lane(Lane.ROTATE).windowRemainingMs());
		assertNull(payload.flipTarget());
		assertFalse(payload.lane(Lane.ROTATE).windowPending());
		assertEquals(RotateAbility.Stance.SCUTUM, payload.stance());
	}

	@Test
	void builderReadsMomentumStacksAndGrace() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		for (int i = 0; i < 3; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, now, 1.0F);
		}

		PlayerStatusPayload payload = PlayerStatusPayload.of(juggernaut(), runtime, now);

		assertEquals(3, payload.momentumStacks());
		assertEquals(5_000, payload.momentumGraceRemainingMs());
		assertEquals(now, payload.momentumGraceStampMs());

		// A second later the stacks hold and only the grace remainder moved.
		PlayerStatusPayload later = PlayerStatusPayload.of(juggernaut(), runtime, now + 1_000);
		assertEquals(3, later.momentumStacks());
		assertEquals(4_000, later.momentumGraceRemainingMs());
		assertEquals(now, later.momentumGraceStampMs());
	}

	@Test
	void movedSinceCatchesAGraceRestampAtTheStackCap() {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < 20; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		}
		PlayerStatusPayload capped = PlayerStatusPayload.of(juggernaut(), runtime, 0);
		assertEquals(20, capped.momentumStacks());

		// A hit at the cap moves neither count nor any armed flag: only the grace stamp.
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 4_000, 1.0F);
		PlayerStatusPayload restamped = PlayerStatusPayload.of(juggernaut(), runtime, 4_000);

		assertEquals(20, restamped.momentumStacks());
		assertEquals(5_000, restamped.momentumGraceRemainingMs());
		assertEquals(4_000, restamped.momentumGraceStampMs());
		assertTrue(restamped.movedSince(capped));
	}

	@Test
	void builderReadsLegionaryFlipAndWindows() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		runtime.rotate(now, RotateAbility.Stance.GLADIUS);
		assertTrue(runtime.arm(Lane.OATH, now, AequilibriumAbility.WINDOW_TICKS * 50L));

		PlayerStatusPayload payload = PlayerStatusPayload.of(legionary(), runtime, now);

		assertEquals(RotateAbility.Stance.GLADIUS, payload.stance());
		assertEquals(5_000, payload.lane(Lane.ROTATE).windowRemainingMs());
		assertEquals(RotateAbility.Stance.GLADIUS, payload.flipTarget());
		assertTrue(payload.lane(Lane.ROTATE).windowPending());
		assertEquals(Lane.OATH.cooldownMs(), payload.lane(Lane.OATH).remainingMs());
		assertEquals(6_000, payload.lane(Lane.OATH).windowRemainingMs());
	}

	@Test
	void builderReadsGamblerOutcomeStreakAndAllIn() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		assertTrue(runtime.roulette(now, RouletteAbility.Outcome.JACKPOT));
		assertTrue(runtime.arm(Lane.ALL_IN, now, AllInAbility.WINDOW_TICKS * 50L));
		runtime.bumpStreak();

		PlayerStatusPayload payload = PlayerStatusPayload.of(gambler(), runtime, now);

		assertEquals(RouletteAbility.Outcome.JACKPOT, payload.rouletteOutcome());
		assertEquals(10_000, payload.lane(Lane.ROULETTE).windowRemainingMs());
		assertEquals(Lane.ROULETTE.cooldownMs(), payload.lane(Lane.ROULETTE).remainingMs());
		assertEquals(15_000, payload.lane(Lane.ALL_IN).windowRemainingMs());
		assertEquals(Lane.ALL_IN.cooldownMs(), payload.lane(Lane.ALL_IN).remainingMs());
		assertEquals(1, payload.streak());
	}

	@Test
	void missingChoiceReadsAsTheEmptyShape() {
		PlayerRuntime runtime = new PlayerRuntime();

		PlayerStatusPayload classless = PlayerStatusPayload.of(null, runtime, 0);
		assertEquals("", classless.classId());
		assertEquals(0, classless.momentumStacks());
		assertEquals(0, classless.momentumGraceRemainingMs());
		// The classless factory is the same shape, and reads no state at all.
		assertEquals(PlayerStatusPayload.classless(), classless);
		assertEquals(PlayerStatusPayload.classless(), PlayerStatusPayload.of(null, null, 0));
	}

	@Test
	void movedSinceIsFalseForQuietDecay() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.arm(Lane.RUSH, 1_000, RushAbility.WINDOW_TICKS * 50L));

		PlayerStatusPayload atFire = PlayerStatusPayload.of(juggernaut(), runtime, 1_000);
		PlayerStatusPayload oneSecondLater = PlayerStatusPayload.of(juggernaut(), runtime, 2_000);

		assertFalse(oneSecondLater.movedSince(atFire));

		PlayerRuntime idle = new PlayerRuntime();
		PlayerStatusPayload ready = PlayerStatusPayload.of(juggernaut(), idle, 1_000);
		assertFalse(PlayerStatusPayload.of(juggernaut(), idle, 9_000).movedSince(ready));
	}

	@Test
	void movedSinceCatchesEveryDiscreteMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload ready = PlayerStatusPayload.of(juggernaut(), runtime, 0);

		assertTrue(ready.movedSince(null));

		// Lane armed.
		assertTrue(runtime.arm(Lane.RUSH, 0, RushAbility.WINDOW_TICKS * 50L));
		PlayerStatusPayload rushed = PlayerStatusPayload.of(juggernaut(), runtime, 0);
		assertTrue(rushed.movedSince(ready));

		// Bonus spent while the lane still runs: a load-flag move.
		assertTrue(runtime.consumeBonus(Lane.RUSH, 0));
		PlayerStatusPayload spent = PlayerStatusPayload.of(juggernaut(), runtime, 0);
		assertTrue(spent.movedSince(rushed));

		// Window expired while the lane still runs: an armed-flag move.
		PlayerStatusPayload windowGone = PlayerStatusPayload.of(juggernaut(), runtime, 5_000);
		assertTrue(windowGone.movedSince(spent));

		// Momentum stack moved.
		PlayerRuntime stacking = new PlayerRuntime();
		PlayerStatusPayload noStacks = PlayerStatusPayload.of(juggernaut(), stacking, 0);
		stacking.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		assertTrue(PlayerStatusPayload.of(juggernaut(), stacking, 0).movedSince(noStacks));

		// Stance flip and Impetus load.
		PlayerRuntime flipping = new PlayerRuntime();
		PlayerStatusPayload scutum = PlayerStatusPayload.of(legionary(), flipping, 0);
		flipping.rotate(0, RotateAbility.Stance.GLADIUS);
		PlayerStatusPayload gladius = PlayerStatusPayload.of(legionary(), flipping, 0);
		assertTrue(gladius.movedSince(scutum));
		assertTrue(gladius.movedSince(PlayerStatusPayload.of(legionary(), flipping, 5_000)));

		// Outcome set, Streak moved, class switched.
		PlayerRuntime spinning = new PlayerRuntime();
		PlayerStatusPayload beforeSpin = PlayerStatusPayload.of(gambler(), spinning, 0);
		assertTrue(spinning.roulette(0, RouletteAbility.Outcome.WARD));
		assertTrue(PlayerStatusPayload.of(gambler(), spinning, 0).movedSince(beforeSpin));
		spinning.bumpStreak();
		assertTrue(PlayerStatusPayload.of(gambler(), spinning, 0).movedSince(beforeSpin));

		assertTrue(PlayerStatusPayload.of(gambler(), spinning, 0).movedSince(ready));

		// All In armed.
		assertTrue(spinning.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L));
		assertTrue(PlayerStatusPayload.of(gambler(), spinning, 0).movedSince(beforeSpin));
	}

	@Test
	void windowAndGraceReadsHitTheirBoundaries() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.arm(Lane.RUSH, 1_000, RushAbility.WINDOW_TICKS * 50L));

		assertEquals(5_000, runtime.state(1_000).lane(Lane.RUSH).windowRemainingMs());
		assertEquals(1, runtime.state(5_999).lane(Lane.RUSH).windowRemainingMs());
		assertEquals(0, runtime.state(6_000).lane(Lane.RUSH).windowRemainingMs());
		assertEquals(0, runtime.state(1_000).lane(Lane.ROTATE).windowRemainingMs());

		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 1_000, 1.0F);
		ConditionalPassive momentum = juggernaut().passiveForTrigger(Trigger.ON_HIT_DEALT);
		assertEquals(5_000, runtime.graceRemaining(momentum, 1_000));
		assertEquals(1, runtime.graceRemaining(momentum, 5_999));
		assertEquals(0, runtime.graceRemaining(momentum, 6_000));
		assertEquals(1_000, runtime.graceStamp(momentum));
		runtime.clear(momentum);
		assertEquals(0, runtime.graceRemaining(momentum, 1_000));
		assertEquals(0, runtime.graceStamp(momentum));

		assertEquals(0, new PlayerRuntime().graceStamp(momentum));
	}
}
