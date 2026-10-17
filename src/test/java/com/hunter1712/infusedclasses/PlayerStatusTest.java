package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
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
		assertTrue(runtime.tryUseRush(now));
		assertTrue(runtime.tryUseRend(now, 7));

		PlayerStatusPayload payload = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, now);

		assertEquals("infusedclasses:juggernaut", payload.classId());
		assertEquals(Cooldowns.RUSH_MS, payload.rushRemainingMs());
		assertEquals(Cooldowns.REND_MS, payload.rendRemainingMs());
		assertEquals(0, payload.rotateRemainingMs());
		assertEquals(0, payload.oathRemainingMs());
		assertEquals(0, payload.rouletteRemainingMs());
		assertEquals(0, payload.allInRemainingMs());
		assertEquals(5_000, payload.rushWindowRemainingMs());
		assertTrue(payload.rushBonusPending());
		assertEquals(6_000, payload.rendWindowRemainingMs());
		assertTrue(payload.rendBonusPending());
		assertEquals(0, payload.flipWindowRemainingMs());
		assertNull(payload.flipTarget());
		assertFalse(payload.impetusPending());
		assertEquals(RotateAbility.Stance.SCUTUM, payload.stance());
	}

	@Test
	void builderReadsMomentumStacksAndGrace() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		for (int i = 0; i < 3; i++) {
			runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, now, 1.0F);
		}

		PlayerStatusPayload payload = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, now);

		assertEquals(3, payload.momentumStacks());
		assertEquals(5_000, payload.momentumGraceRemainingMs());
		assertEquals(now, payload.momentumGraceStampMs());

		// A second later the stacks hold and only the grace remainder moved.
		PlayerStatusPayload later = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, now + 1_000);
		assertEquals(3, later.momentumStacks());
		assertEquals(4_000, later.momentumGraceRemainingMs());
		assertEquals(now, later.momentumGraceStampMs());
	}

	@Test
	void movedSinceCatchesAGraceRestampAtTheStackCap() {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < 20; i++) {
			runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);
		}
		PlayerStatusPayload capped = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 0);
		assertEquals(20, capped.momentumStacks());

		// A hit at the cap moves neither count nor any armed flag: only the grace stamp.
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 4_000, 1.0F);
		PlayerStatusPayload restamped = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 4_000);

		assertEquals(20, restamped.momentumStacks());
		assertEquals(5_000, restamped.momentumGraceRemainingMs());
		assertEquals(4_000, restamped.momentumGraceStampMs());
		assertTrue(restamped.movedSince(capped));
	}

	@Test
	void builderReadsLegionaryFlipAndWindows() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		runtime.flipStance(now, RotateAbility.Stance.GLADIUS);
		assertTrue(runtime.tryUseAequilibrium(now));

		PlayerStatusPayload payload = PlayerStatusPayload.of(ClassRegistry.LEGIONARY, runtime, now);

		assertEquals(RotateAbility.Stance.GLADIUS, payload.stance());
		assertEquals(5_000, payload.flipWindowRemainingMs());
		assertEquals(RotateAbility.Stance.GLADIUS, payload.flipTarget());
		assertTrue(payload.impetusPending());
		assertEquals(Cooldowns.OATH_MS, payload.oathRemainingMs());
		assertEquals(6_000, payload.aequilibriumWindowRemainingMs());
	}

	@Test
	void builderReadsGamblerOutcomeStreakAndAllIn() {
		PlayerRuntime runtime = new PlayerRuntime();
		long now = 1_000L;
		assertTrue(runtime.tryUseRoulette(now, RouletteAbility.Outcome.JACKPOT));
		assertTrue(runtime.tryUseAllIn(now));
		runtime.bumpStreak();

		PlayerStatusPayload payload = PlayerStatusPayload.of(ClassRegistry.GAMBLER, runtime, now);

		assertEquals(RouletteAbility.Outcome.JACKPOT, payload.rouletteOutcome());
		assertEquals(10_000, payload.rouletteWindowRemainingMs());
		assertEquals(Cooldowns.ROULETTE_MS, payload.rouletteRemainingMs());
		assertEquals(15_000, payload.allInWindowRemainingMs());
		assertEquals(Cooldowns.ALL_IN_MS, payload.allInRemainingMs());
		assertEquals(1, payload.streak());
	}

	@Test
	void missingChoiceReadsEmptyAndUnknownTravelsVerbatim() {
		PlayerRuntime runtime = new PlayerRuntime();

		PlayerStatusPayload classless = PlayerStatusPayload.of(null, runtime, 0);
		assertEquals("", classless.classId());
		assertEquals(0, classless.momentumStacks());
		assertEquals(0, classless.momentumGraceRemainingMs());

		PlayerStatusPayload unknown = PlayerStatusPayload.of(
				Identifier.fromNamespaceAndPath("infusedclasses", "nope"), runtime, 0);
		assertEquals("infusedclasses:nope", unknown.classId());
		assertEquals(0, unknown.momentumStacks());
	}

	@Test
	void movedSinceIsFalseForQuietDecay() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.tryUseRush(1_000));

		PlayerStatusPayload atFire = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 1_000);
		PlayerStatusPayload oneSecondLater = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 2_000);

		assertFalse(oneSecondLater.movedSince(atFire));

		PlayerRuntime idle = new PlayerRuntime();
		PlayerStatusPayload ready = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, idle, 1_000);
		assertFalse(PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, idle, 9_000).movedSince(ready));
	}

	@Test
	void movedSinceCatchesEveryDiscreteMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload ready = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 0);

		assertTrue(ready.movedSince(null));

		// Lane armed.
		assertTrue(runtime.tryUseRush(0));
		PlayerStatusPayload rushed = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 0);
		assertTrue(rushed.movedSince(ready));

		// Bonus spent while the lane still runs: a load-flag move.
		assertTrue(runtime.consumeRushBonus(0));
		PlayerStatusPayload spent = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 0);
		assertTrue(spent.movedSince(rushed));

		// Window expired while the lane still runs: an armed-flag move.
		PlayerStatusPayload windowGone = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, 5_000);
		assertTrue(windowGone.movedSince(spent));

		// Momentum stack moved.
		PlayerRuntime stacking = new PlayerRuntime();
		PlayerStatusPayload noStacks = PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, stacking, 0);
		stacking.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);
		assertTrue(PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, stacking, 0).movedSince(noStacks));

		// Stance flip and Impetus load.
		PlayerRuntime flipping = new PlayerRuntime();
		PlayerStatusPayload scutum = PlayerStatusPayload.of(ClassRegistry.LEGIONARY, flipping, 0);
		flipping.flipStance(0, RotateAbility.Stance.GLADIUS);
		PlayerStatusPayload gladius = PlayerStatusPayload.of(ClassRegistry.LEGIONARY, flipping, 0);
		assertTrue(gladius.movedSince(scutum));
		assertTrue(gladius.movedSince(PlayerStatusPayload.of(ClassRegistry.LEGIONARY, flipping, 5_000)));

		// Outcome set, Streak moved, class switched.
		PlayerRuntime spinning = new PlayerRuntime();
		PlayerStatusPayload beforeSpin = PlayerStatusPayload.of(ClassRegistry.GAMBLER, spinning, 0);
		assertTrue(spinning.tryUseRoulette(0, RouletteAbility.Outcome.WARD));
		assertTrue(PlayerStatusPayload.of(ClassRegistry.GAMBLER, spinning, 0).movedSince(beforeSpin));
		spinning.bumpStreak();
		assertTrue(PlayerStatusPayload.of(ClassRegistry.GAMBLER, spinning, 0).movedSince(beforeSpin));

		assertTrue(PlayerStatusPayload.of(ClassRegistry.GAMBLER, spinning, 0).movedSince(ready));

		// All In armed.
		assertTrue(spinning.tryUseAllIn(0));
		assertTrue(PlayerStatusPayload.of(ClassRegistry.GAMBLER, spinning, 0).movedSince(beforeSpin));
	}

	@Test
	void windowAndGraceReadsHitTheirBoundaries() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.tryUseRush(1_000));

		assertEquals(5_000, runtime.rushWindowRemaining(1_000));
		assertEquals(1, runtime.rushWindowRemaining(5_999));
		assertEquals(0, runtime.rushWindowRemaining(6_000));
		assertEquals(0, runtime.flipWindowRemaining(1_000));

		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 1_000, 1.0F);
		ConditionalPassive momentum = juggernaut().passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
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
