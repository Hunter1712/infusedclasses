package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ability roster: one row per fired ability, carrying the lane, the word, the gating
 * id and the stack's view. Pins that every registered active has a row, that both lane
 * reads derive from the row's lane, and that the class's shape draws the rows the slots
 * fire.
 */
class AbilityRosterTest {
	@Test
	void everyRegisteredActiveHasARow() {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (ClassDefinition.ActiveAbility active : def.actives()) {
				assertTrue(AbilityRoster.byAbility(active.id()) != null, active.id().toString());
			}
		}
	}

	@Test
	void rowsAreTheSixAbilitiesInLaneOrder() {
		assertEquals(
				List.of(ClassRegistry.RUSH_ABILITY, ClassRegistry.REND_ABILITY, ClassRegistry.ROTATE_ABILITY,
						ClassRegistry.AEQUILIBRIUM_ABILITY, ClassRegistry.ROULETTE_ABILITY, ClassRegistry.ALL_IN_ABILITY),
				AbilityRoster.all().stream().map(AbilityRoster.Row::ability).toList());
		assertEquals(
				List.of("Rush", "Rend", "Rotate", "Aequilibrium", "Roulette", "All In"),
				AbilityRoster.all().stream().map(AbilityRoster.Row::name).toList());
		assertEquals(
				List.of(Lane.RUSH, Lane.REND, Lane.ROTATE, Lane.OATH, Lane.ROULETTE, Lane.ALL_IN),
				AbilityRoster.all().stream().map(AbilityRoster.Row::lane).toList());
	}

	@Test
	void rowsReadTheirOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.arm(Lane.RUSH, 0, RushAbility.WINDOW_TICKS * 50L);
		runtime.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 1);
		runtime.rotate(0, RotateAbility.Stance.GLADIUS);
		runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L);
		runtime.roulette(0, RouletteAbility.Outcome.EDGE);
		runtime.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L);
		PlayerRuntime.State state = runtime.state(0);

		assertEquals(Lane.RUSH.cooldownMs(), AbilityRoster.RUSH.laneRemaining(state));
		assertEquals(Lane.REND.cooldownMs(), AbilityRoster.REND.laneRemaining(state));
		assertEquals(Lane.ROTATE.cooldownMs(), AbilityRoster.ROTATE.laneRemaining(state));
		assertEquals(Lane.OATH.cooldownMs(), AbilityRoster.AEQUILIBRIUM.laneRemaining(state));
		assertEquals(Lane.ROULETTE.cooldownMs(), AbilityRoster.ROULETTE.laneRemaining(state));
		assertEquals(Lane.ALL_IN.cooldownMs(), AbilityRoster.ALL_IN.laneRemaining(state));
	}

	@Test
	void shapeDrawsTheRowsTheSlotsFire() {
		assertEquals(AbilityRoster.RUSH, BindMorph.shape(juggernaut()).signatureRow());
		assertEquals(AbilityRoster.REND, BindMorph.shape(juggernaut()).ultimateRow());
		assertEquals(AbilityRoster.ROTATE, BindMorph.shape(legionary()).signatureRow(),
				"the ultimate no longer keys on the ROTATE row: the draw and the fire are one walk");
		assertEquals(AbilityRoster.AEQUILIBRIUM, BindMorph.shape(legionary()).ultimateRow());
		assertEquals(AbilityRoster.ROULETTE, BindMorph.shape(gambler()).signatureRow());
		assertEquals(AbilityRoster.ALL_IN, BindMorph.shape(gambler()).ultimateRow());

		// A class carrying neither slot's abilities draws neither line, and the
		// printed fallbacks still pick the keys up for everyone else.
		ClassDefinition mystery = new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "mystery"),
				"Mystery", List.of(), List.of(), 0, "", List.of(), List.of(), CritRule.universal());
		assertNull(BindMorph.shape(mystery).signatureRow());
		assertNull(BindMorph.shape(mystery).ultimateRow());
		assertNull(BindMorph.shape(null).signatureRow());
	}

	@Test
	void unknownIdsFindNoRow() {
		assertNull(AbilityRoster.byAbility(
				Identifier.fromNamespaceAndPath("infusedclasses", "mystery")));
	}

	@Test
	void laneReadsAgreeWithTheRuntime() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.arm(Lane.RUSH, 0, RushAbility.WINDOW_TICKS * 50L);
		runtime.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 1);
		runtime.rotate(0, RotateAbility.Stance.GLADIUS);
		runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L);
		runtime.roulette(1_000, RouletteAbility.Outcome.EDGE);
		runtime.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L);
		long now = 1_000;
		PlayerRuntime.State state = runtime.state(now);
		PlayerStatusPayload status = PlayerStatusPayload.of(juggernaut(), runtime, now);

		for (AbilityRoster.Row row : AbilityRoster.all()) {
			long fromPayload = row.laneRemaining(status);
			assertTrue(fromPayload > 0, row.name());
			assertEquals(row.laneRemaining(state), fromPayload, row.name());
		}
	}

	@Test
	void ridersReadTheirOwnWindow() {
		PlayerStatusPayload status = windows(11_000, 12_000, 13_000, 14_000, 15_000, 16_000);

		assertRidersRead(AbilityRoster.RUSH, status, 11_000);
		assertRidersRead(AbilityRoster.REND, status, 12_000);
		assertRidersRead(AbilityRoster.ROTATE, status, 13_000);
		assertRidersRead(AbilityRoster.ROULETTE, status, 14_000);
		assertRidersRead(AbilityRoster.AEQUILIBRIUM, status, 15_000);
		assertRidersRead(AbilityRoster.ALL_IN, status, 16_000);
	}

	/** Every rider of the row reads the one window it is keyed to. */
	private static void assertRidersRead(AbilityRoster.Row row, PlayerStatusPayload status, long window) {
		assertFalse(row.riders().isEmpty(), row.name());
		for (AbilityRoster.Rider rider : row.riders()) {
			assertEquals(window, rider.remaining().applyAsLong(status), row.name());
		}
	}

	@Test
	void riderRulesFollowTheirFlags() {
		// The Rotate riders split by what the flip landed on: Impetus when it is pending,
		// the Testudo catch while the window runs in scutum.
		PlayerRuntime gladius = new PlayerRuntime();
		gladius.rotate(0, RotateAbility.Stance.GLADIUS);
		PlayerStatusPayload flippedToGladius = snapshot(gladius);
		assertTrue(AbilityRoster.ROTATE.riders().get(0).wanted().test(flippedToGladius));
		assertFalse(AbilityRoster.ROTATE.riders().get(1).wanted().test(flippedToGladius));

		PlayerRuntime scutum = new PlayerRuntime();
		scutum.rotate(0, RotateAbility.Stance.SCUTUM);
		PlayerStatusPayload flippedToScutum = snapshot(scutum);
		assertFalse(AbilityRoster.ROTATE.riders().get(0).wanted().test(flippedToScutum));
		assertTrue(AbilityRoster.ROTATE.riders().get(1).wanted().test(flippedToScutum));

		// Zero is the dead band and draws nothing; a live band draws its own word.
		PlayerRuntime zero = new PlayerRuntime();
		zero.roulette(0, RouletteAbility.Outcome.ZERO);
		assertFalse(AbilityRoster.ROULETTE.riders().get(0).wanted().test(snapshot(zero)));

		PlayerRuntime edge = new PlayerRuntime();
		edge.roulette(0, RouletteAbility.Outcome.EDGE);
		PlayerStatusPayload spun = snapshot(edge);
		assertTrue(AbilityRoster.ROULETTE.riders().get(0).wanted().test(spun));
		assertEquals("Edge", AbilityRoster.ROULETTE.riders().get(0).label().apply(spun));

		// The Rend rider exists exactly while its next-hit snapshot is unspent.
		assertFalse(AbilityRoster.REND.riders().get(0).wanted().test(snapshot(new PlayerRuntime())));
		PlayerRuntime holding = new PlayerRuntime();
		holding.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 3);
		assertTrue(AbilityRoster.REND.riders().get(0).wanted().test(snapshot(holding)));
	}

	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	/** A snapshot with only the six windows set, all distinct, so a rider reading the wrong one fails. */
	private static PlayerStatusPayload windows(long rush, long rend, long flip, long roulette, long aeq, long allIn) {
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			lanes.put(lane, new LaneRead(0, 0, false));
		}
		lanes.put(Lane.RUSH, new LaneRead(0, rush, false));
		lanes.put(Lane.REND, new LaneRead(0, rend, true));
		lanes.put(Lane.ROTATE, new LaneRead(0, flip, false));
		lanes.put(Lane.ROULETTE, new LaneRead(0, roulette, false));
		lanes.put(Lane.OATH, new LaneRead(0, aeq, false));
		lanes.put(Lane.ALL_IN, new LaneRead(0, allIn, false));
		return new PlayerStatusPayload("infusedclasses:juggernaut", lanes, RotateAbility.Stance.SCUTUM,
				RouletteAbility.Outcome.EDGE, 0, 0, 0, null, 0);
	}

	/** The snapshot a server would send for this runtime; the class does not matter to what rows read. */
	private static PlayerStatusPayload snapshot(PlayerRuntime runtime) {
		return PlayerStatusPayload.of(juggernaut(), runtime, 0);
	}
}
