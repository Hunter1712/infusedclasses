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

	/** The server's shape: one state read at {@code now}, then the payload built from it. */
	private static PlayerStatusPayload snapshot(ClassDefinition def, PlayerRuntime runtime, long now) {
		return PlayerStatusPayload.of(def, runtime, runtime == null ? null : runtime.state(now), now);
	}

	@Test
	void builderReadsLanesWindowsAndLoadFlags() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		assertTrue(runtime.arm(Lane.OFFLOAD, now, OffloadAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.arm(Lane.ONSLAUGHT, now, OnslaughtAbility.WINDOW_TICKS * 50L));

		PlayerStatusPayload payload = snapshot(juggernaut(), runtime, now);

		assertEquals("infusedclasses:juggernaut", payload.classId());
		assertEquals(Lane.OFFLOAD.cooldownMs(), payload.lane(Lane.OFFLOAD).remainingMs());
		assertEquals(Lane.ONSLAUGHT.cooldownMs(), payload.lane(Lane.ONSLAUGHT).remainingMs());
		assertEquals(0, payload.lane(Lane.ROTATE).remainingMs());
		assertEquals(0, payload.lane(Lane.OATH).remainingMs());
		assertEquals(0, payload.lane(Lane.ROULETTE).remainingMs());
		assertEquals(0, payload.lane(Lane.ALL_IN).remainingMs());
		assertEquals(5_000, payload.lane(Lane.OFFLOAD).windowRemainingMs());
		assertTrue(payload.lane(Lane.OFFLOAD).windowPending());
		assertEquals(15_000, payload.lane(Lane.ONSLAUGHT).windowRemainingMs());
		assertTrue(payload.lane(Lane.ONSLAUGHT).windowPending());
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

		PlayerStatusPayload payload = snapshot(juggernaut(), runtime, now);

		assertEquals(3, payload.momentumStacks());
		assertEquals(5_000, payload.momentumGraceRemainingMs());
		assertEquals(now, payload.momentumGraceStampMs());

		// A second later the stacks hold and only the grace remainder moved.
		PlayerStatusPayload later = snapshot(juggernaut(), runtime, now + 1_000);
		assertEquals(3, later.momentumStacks());
		assertEquals(4_000, later.momentumGraceRemainingMs());
		assertEquals(now, later.momentumGraceStampMs());
	}

	@Test
	void snapshotComposesTheRuntimeReads() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, now, 1.0F);
		ConditionalPassive momentum = juggernaut().passiveForTrigger(Trigger.ON_HIT_DEALT);

		PlayerRuntime.State state = runtime.state(now);
		PlayerStatusPayload payload = PlayerStatusPayload.of(juggernaut(), runtime, state, now);

		// The payload is the two reads it composes, not a field-by-field restatement.
		assertEquals(state, payload.state());
		assertEquals(runtime.bar(momentum, now), payload.bar());
	}

	@Test
	void movedSinceCatchesAGraceRestampAtTheStackCap() {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < Momentum.MAX_STACKS; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		}
		PlayerStatusPayload capped = snapshot(juggernaut(), runtime, 0);
		assertEquals(Momentum.MAX_STACKS, capped.momentumStacks());

		// A hit at the cap moves neither count nor any armed flag: only the grace stamp.
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 4_000, 1.0F);
		PlayerStatusPayload restamped = snapshot(juggernaut(), runtime, 4_000);

		assertEquals(Momentum.MAX_STACKS, restamped.momentumStacks());
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

		PlayerStatusPayload payload = snapshot(legionary(), runtime, now);

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

		PlayerStatusPayload payload = snapshot(gambler(), runtime, now);

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

		PlayerStatusPayload classless = snapshot(null, runtime, 0);
		assertEquals("", classless.classId());
		assertEquals(0, classless.momentumStacks());
		assertEquals(0, classless.momentumGraceRemainingMs());
		// The classless factory is the same shape, and reads no state at all.
		assertEquals(PlayerStatusPayload.classless(), classless);
		assertEquals(PlayerStatusPayload.classless(), snapshot(null, null, 0));
	}

	@Test
	void movedSinceIsFalseForQuietDecay() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.arm(Lane.OFFLOAD, 1_000, OffloadAbility.WINDOW_TICKS * 50L));

		PlayerStatusPayload atFire = snapshot(juggernaut(), runtime, 1_000);
		PlayerStatusPayload oneSecondLater = snapshot(juggernaut(), runtime, 2_000);

		assertFalse(oneSecondLater.movedSince(atFire));

		PlayerRuntime idle = new PlayerRuntime();
		PlayerStatusPayload ready = snapshot(juggernaut(), idle, 1_000);
		assertFalse(snapshot(juggernaut(), idle, 9_000).movedSince(ready));
	}

	@Test
	void movedSinceCatchesEveryDiscreteMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload ready = snapshot(juggernaut(), runtime, 0);

		assertTrue(ready.movedSince(null));

		// Lane armed.
		assertTrue(runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L));
		PlayerStatusPayload armed = snapshot(juggernaut(), runtime, 0);
		assertTrue(armed.movedSince(ready));

		// Window expired while the lane still runs: an armed-flag move.
		PlayerStatusPayload windowGone = snapshot(juggernaut(), runtime, 5_000);
		assertTrue(windowGone.movedSince(armed));

		// Momentum stack moved.
		PlayerRuntime stacking = new PlayerRuntime();
		PlayerStatusPayload noStacks = snapshot(juggernaut(), stacking, 0);
		stacking.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		assertTrue(snapshot(juggernaut(), stacking, 0).movedSince(noStacks));

		// Stance flip and Impetus load.
		PlayerRuntime flipping = new PlayerRuntime();
		PlayerStatusPayload scutum = snapshot(legionary(), flipping, 0);
		flipping.rotate(0, RotateAbility.Stance.GLADIUS);
		PlayerStatusPayload gladius = snapshot(legionary(), flipping, 0);
		assertTrue(gladius.movedSince(scutum));
		assertTrue(gladius.movedSince(snapshot(legionary(), flipping, 5_000)));

		// Outcome set, Streak moved, class switched.
		PlayerRuntime spinning = new PlayerRuntime();
		PlayerStatusPayload beforeSpin = snapshot(gambler(), spinning, 0);
		assertTrue(spinning.roulette(0, RouletteAbility.Outcome.WARD));
		assertTrue(snapshot(gambler(), spinning, 0).movedSince(beforeSpin));
		spinning.bumpStreak();
		assertTrue(snapshot(gambler(), spinning, 0).movedSince(beforeSpin));

		assertTrue(snapshot(gambler(), spinning, 0).movedSince(ready));

		// All In armed.
		assertTrue(spinning.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L));
		assertTrue(snapshot(gambler(), spinning, 0).movedSince(beforeSpin));
	}

	@Test
	void windowAndGraceReadsHitTheirBoundaries() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.arm(Lane.OFFLOAD, 1_000, OffloadAbility.WINDOW_TICKS * 50L));

		assertEquals(5_000, runtime.state(1_000).lane(Lane.OFFLOAD).windowRemainingMs());
		assertEquals(1, runtime.state(5_999).lane(Lane.OFFLOAD).windowRemainingMs());
		assertEquals(0, runtime.state(6_000).lane(Lane.OFFLOAD).windowRemainingMs());
		assertEquals(0, runtime.state(1_000).lane(Lane.ROTATE).windowRemainingMs());

		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 1_000, 1.0F);
		ConditionalPassive momentum = juggernaut().passiveForTrigger(Trigger.ON_HIT_DEALT);
		assertEquals(5_000, runtime.bar(momentum, 1_000).graceRemainingMs());
		assertEquals(1, runtime.bar(momentum, 5_999).graceRemainingMs());
		assertEquals(0, runtime.bar(momentum, 6_000).graceRemainingMs());
		assertEquals(1_000, runtime.bar(momentum, 1_000).graceStampMs());
		runtime.spendStacks(momentum, 1);
		assertEquals(0, runtime.bar(momentum, 1_000).graceRemainingMs());
		assertEquals(0, runtime.bar(momentum, 1_000).graceStampMs());

		assertEquals(0, new PlayerRuntime().bar(momentum, 0).graceStampMs());
	}
}
