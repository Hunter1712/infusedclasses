package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The status snapshot decision: built from the transient runtime and a {@code now},
 * asserted without a player, a level or a clock. The payload's {@code movedSince}
 * guard is the whole reason the strip costs no packets between moves.
 */
class PlayerStatusTest {
	private static final long NOW = 1_000_000L;

	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ConditionalPassive momentum() {
		return juggernaut().passives().get(0);
	}

	private static PlayerStatusPayload of(PlayerRuntime runtime, long now) {
		return PlayerStatusPayload.of(ClassRegistry.JUGGERNAUT, runtime, now);
	}

	@Test
	void freshJuggernautReadsReadyAndEmpty() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload status = of(runtime, NOW);

		assertEquals("infusedclasses:juggernaut", status.classId());
		assertEquals(0, status.rushRemainingMs());
		assertEquals(0, status.rendRemainingMs());
		assertEquals(0, status.rushWindowRemainingMs());
		assertFalse(status.rushBonusPending());
		assertEquals(0, status.rendWindowRemainingMs());
		assertFalse(status.rendBonusPending());
		assertEquals(0, status.flipWindowRemainingMs());
		assertNull(status.flipTarget());
		assertFalse(status.impetusPending());
		assertEquals(0, status.momentumStacks());
		assertEquals(0, status.momentumGraceRemainingMs());
		assertEquals(RotateAbility.Stance.SCUTUM, status.stance());
		assertEquals(0, status.streak());
	}

	@Test
	void rushFireArmsLaneWindowAndBonus() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, NOW);
		PlayerStatusPayload status = of(runtime, NOW);

		assertEquals(Cooldowns.RUSH_MS, status.rushRemainingMs());
		assertEquals(RushAbility.WINDOW_TICKS * 50L, status.rushWindowRemainingMs());
		assertTrue(status.rushBonusPending());
	}

	@Test
	void rendFireArmsLaneWindowAndEmpower() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.bumpPassive(momentum(), 3, NOW);
		RendAbility.fire(juggernaut(), runtime, NOW);
		PlayerStatusPayload status = of(runtime, NOW);

		assertEquals(Cooldowns.REND_MS, status.rendRemainingMs());
		assertEquals(RendAbility.WINDOW_TICKS * 50L, status.rendWindowRemainingMs());
		assertTrue(status.rendBonusPending());
		assertEquals(3, status.momentumStacks());
	}

	@Test
	void spentOneShotsReadLoadedFalseWhileTheirWindowsStayLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRush(NOW);
		runtime.tryUseRend(NOW, 5);
		runtime.tryUseRotate(NOW);
		runtime.flipStance(NOW, RotateAbility.Stance.GLADIUS);
		runtime.consumeRushBonus(NOW);
		runtime.consumeRendEmpower(NOW);
		runtime.consumeImpetusBonus(NOW);

		PlayerStatusPayload status = of(runtime, NOW);

		assertFalse(status.rushBonusPending());
		assertTrue(status.rushWindowRemainingMs() > 0);
		assertFalse(status.rendBonusPending());
		assertTrue(status.rendWindowRemainingMs() > 0);
		assertFalse(status.impetusPending());
		assertTrue(status.flipWindowRemainingMs() > 0);
		assertEquals(RotateAbility.Stance.GLADIUS, status.flipTarget());
	}

	@Test
	void legionaryFlipCarriesTargetAndWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		RotateAbility.fire(legionary(), runtime, NOW);
		PlayerStatusPayload status = PlayerStatusPayload.of(ClassRegistry.LEGIONARY, runtime, NOW);

		assertEquals("infusedclasses:legionary", status.classId());
		assertEquals(Cooldowns.ROTATE_MS, status.rotateRemainingMs());
		assertEquals(RotateAbility.SWITCH_DEAL_MS, status.flipWindowRemainingMs());
		assertEquals(RotateAbility.Stance.GLADIUS, status.flipTarget());
		assertEquals(RotateAbility.Stance.GLADIUS, status.stance());
		assertTrue(status.impetusPending());
	}

	@Test
	void aequilibriumArmsTheOathLaneAndItsWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		AequilibriumAbility.fire(legionary(), runtime, NOW);
		PlayerStatusPayload status = PlayerStatusPayload.of(ClassRegistry.LEGIONARY, runtime, NOW);

		assertEquals(Cooldowns.OATH_MS, status.oathRemainingMs());
		assertEquals(AequilibriumAbility.WINDOW_TICKS * 50L, status.aequilibriumWindowRemainingMs());
	}

	@Test
	void gamblerSpinCarriesOutcomeAndWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRoulette(NOW, RouletteAbility.Outcome.HASTE);
		PlayerStatusPayload status = PlayerStatusPayload.of(ClassRegistry.GAMBLER, runtime, NOW);

		assertEquals("infusedclasses:gambler", status.classId());
		assertEquals(Cooldowns.ROULETTE_MS, status.rouletteRemainingMs());
		assertEquals(RouletteAbility.WINDOW_TICKS * 50L, status.rouletteWindowRemainingMs());
		assertEquals(RouletteAbility.Outcome.HASTE, status.rouletteOutcome());
	}

	@Test
	void allInCarriesItsLaneWindowAndStreak() {
		PlayerRuntime runtime = new PlayerRuntime();
		AllInAbility.fire(ClassRegistry.byId(ClassRegistry.GAMBLER), runtime, NOW);
		runtime.bumpStreak();
		PlayerStatusPayload status = PlayerStatusPayload.of(ClassRegistry.GAMBLER, runtime, NOW);

		assertEquals(Cooldowns.ALL_IN_MS, status.allInRemainingMs());
		assertEquals(AllInAbility.WINDOW_TICKS * 50L, status.allInWindowRemainingMs());
		assertEquals(1, status.streak());
	}

	@Test
	void stacksCarryTheirGraceCountdown() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);

		PlayerStatusPayload fresh = of(runtime, NOW);
		assertEquals(2, fresh.momentumStacks());
		assertEquals(5_000, fresh.momentumGraceRemainingMs());

		assertEquals(4_001, of(runtime, NOW + 999).momentumGraceRemainingMs());
	}

	@Test
	void expiredWindowsReadZeroAtTheBoundary() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, NOW);

		assertEquals(1, runtime.rushWindowRemaining(NOW + 4_999));
		assertEquals(0, runtime.rushWindowRemaining(NOW + 5_000));
		assertEquals(1, of(runtime, NOW + 4_999).rushWindowRemainingMs());
		assertEquals(0, of(runtime, NOW + 5_000).rushWindowRemainingMs());
		assertFalse(of(runtime, NOW + 5_000).rushBonusPending());
	}

	@Test
	void classlessCarriesTheEmptyClassId() {
		assertEquals("", PlayerStatusPayload.of(null, new PlayerRuntime(), NOW).classId());
	}

	@Test
	void noPreviousSnapshotIsAlwaysAMove() {
		assertTrue(of(new PlayerRuntime(), NOW).movedSince(null));
	}

	@Test
	void quietSecondIsNoMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, NOW);
		PlayerStatusPayload first = of(runtime, NOW);

		PlayerStatusPayload oneSecondLater = of(runtime, NOW + 1_000);

		assertFalse(oneSecondLater.movedSince(first));
	}

	@Test
	void laneAndWindowArmedIsAMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload before = of(runtime, NOW);
		RushAbility.fire(juggernaut(), runtime, NOW);

		assertTrue(of(runtime, NOW).movedSince(before));
	}

	@Test
	void stackBumpIsAMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload before = of(runtime, NOW);
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);

		assertTrue(of(runtime, NOW).movedSince(before));
	}

	@Test
	void streakMoveIsAMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload before = PlayerStatusPayload.of(ClassRegistry.GAMBLER, runtime, NOW);
		runtime.bumpStreak();

		assertTrue(PlayerStatusPayload.of(ClassRegistry.GAMBLER, runtime, NOW).movedSince(before));
	}

	@Test
	void stanceFlipAndOutcomeSetAreMoves() {
		PlayerRuntime legionRuntime = new PlayerRuntime();
		PlayerStatusPayload scutum = PlayerStatusPayload.of(ClassRegistry.LEGIONARY, legionRuntime, NOW);
		legionRuntime.flipStance(NOW, RotateAbility.Stance.GLADIUS);

		assertTrue(PlayerStatusPayload.of(ClassRegistry.LEGIONARY, legionRuntime, NOW).movedSince(scutum));

		PlayerRuntime gamblerRuntime = new PlayerRuntime();
		PlayerStatusPayload noOutcome = PlayerStatusPayload.of(ClassRegistry.GAMBLER, gamblerRuntime, NOW);
		gamblerRuntime.tryUseRoulette(NOW, RouletteAbility.Outcome.EDGE);

		assertTrue(PlayerStatusPayload.of(ClassRegistry.GAMBLER, gamblerRuntime, NOW).movedSince(noOutcome));
	}

	@Test
	void classSwitchIsAMove() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload juggernaut = of(runtime, NOW);

		assertTrue(PlayerStatusPayload.of(ClassRegistry.LEGIONARY, runtime, NOW).movedSince(juggernaut));
	}
}
