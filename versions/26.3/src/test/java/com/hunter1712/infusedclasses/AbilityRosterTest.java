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
				List.of(ClassRegistry.OFFLOAD_ABILITY, ClassRegistry.ONSLAUGHT_ABILITY, ClassRegistry.ROTATE_ABILITY,
						ClassRegistry.AEQUILIBRIUM_ABILITY, ClassRegistry.ROULETTE_ABILITY, ClassRegistry.ALL_IN_ABILITY),
				AbilityRoster.all().stream().map(AbilityRoster.Row::ability).toList());
		assertEquals(
				List.of("Offload", "Onslaught", "Rotate", "Aequilibrium", "Roulette", "All In"),
				AbilityRoster.all().stream().map(AbilityRoster.Row::name).toList());
		assertEquals(
				List.of(Lane.OFFLOAD, Lane.ONSLAUGHT, Lane.ROTATE, Lane.OATH, Lane.ROULETTE, Lane.ALL_IN),
				AbilityRoster.all().stream().map(AbilityRoster.Row::lane).toList());
	}

	@Test
	void rowsCarryTheirSlot() {
		assertEquals(AbilityRoster.Slot.SIGNATURE, AbilityRoster.OFFLOAD.slot());
		assertEquals(AbilityRoster.Slot.SIGNATURE, AbilityRoster.ROTATE.slot());
		assertEquals(AbilityRoster.Slot.SIGNATURE, AbilityRoster.ROULETTE.slot());
		assertEquals(AbilityRoster.Slot.ULTIMATE, AbilityRoster.ONSLAUGHT.slot());
		assertEquals(AbilityRoster.Slot.ULTIMATE, AbilityRoster.AEQUILIBRIUM.slot());
		assertEquals(AbilityRoster.Slot.ULTIMATE, AbilityRoster.ALL_IN.slot());
	}

	@Test
	void aRowFiresTheAbilityItNames() {
		PlayerRuntime runtime = new PlayerRuntime();

		AbilityOutcome rotated = AbilityRoster.ROTATE.fire().fire(legionary(), runtime, 0, 0.5F);
		assertTrue(rotated.fired());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.state(0).stance());
		assertEquals("Gladius!", rotated.announcement().text());

		// The wheel reads the pressed roll through the same one-argument-shape fire.
		AbilityOutcome spun = AbilityRoster.ROULETTE.fire().fire(gambler(), runtime, 0, 0.5F);
		assertTrue(spun.fired());
		assertEquals(RouletteAbility.Outcome.HASTE, runtime.state(0).rouletteOutcome());
	}

	@Test
	void rowsReadTheirOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L);
		runtime.arm(Lane.ONSLAUGHT, 0, OnslaughtAbility.WINDOW_TICKS * 50L);
		runtime.rotate(0, RotateAbility.Stance.GLADIUS);
		runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L);
		runtime.roulette(0, RouletteAbility.Outcome.EDGE);
		runtime.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L);
		PlayerRuntime.State state = runtime.state(0);

		assertEquals(Lane.OFFLOAD.cooldownMs(), AbilityRoster.OFFLOAD.laneRemaining(state));
		assertEquals(Lane.ONSLAUGHT.cooldownMs(), AbilityRoster.ONSLAUGHT.laneRemaining(state));
		assertEquals(Lane.ROTATE.cooldownMs(), AbilityRoster.ROTATE.laneRemaining(state));
		assertEquals(Lane.OATH.cooldownMs(), AbilityRoster.AEQUILIBRIUM.laneRemaining(state));
		assertEquals(Lane.ROULETTE.cooldownMs(), AbilityRoster.ROULETTE.laneRemaining(state));
		assertEquals(Lane.ALL_IN.cooldownMs(), AbilityRoster.ALL_IN.laneRemaining(state));
	}

	@Test
	void shapeDrawsTheRowsTheSlotsFire() {
		assertEquals(AbilityRoster.OFFLOAD, BindMorph.shape(juggernaut()).signatureRow());
		assertEquals(AbilityRoster.ONSLAUGHT, BindMorph.shape(juggernaut()).ultimateRow());
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
		runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L);
		runtime.arm(Lane.ONSLAUGHT, 0, OnslaughtAbility.WINDOW_TICKS * 50L);
		runtime.rotate(0, RotateAbility.Stance.GLADIUS);
		runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L);
		runtime.roulette(1_000, RouletteAbility.Outcome.EDGE);
		runtime.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L);
		long now = 1_000;
		PlayerRuntime.State state = runtime.state(now);
		PlayerStatusPayload status = PlayerStatusPayload.of(juggernaut(), runtime, state, now);

		for (AbilityRoster.Row row : AbilityRoster.all()) {
			long fromPayload = row.laneRemaining(status);
			assertTrue(fromPayload > 0, row.name());
			assertEquals(row.laneRemaining(state), fromPayload, row.name());
		}
	}

	@Test
	void ridersReadTheirOwnWindow() {
		PlayerStatusPayload status = windows(21_000, 22_000, 23_000, 24_000, 25_000, 26_000);

		assertRidersRead(AbilityRoster.OFFLOAD, status, 21_000);
		assertRidersRead(AbilityRoster.ONSLAUGHT, status, 22_000);
		assertRidersRead(AbilityRoster.ROTATE, status, 23_000);
		assertRidersRead(AbilityRoster.ROULETTE, status, 24_000);
		assertRidersRead(AbilityRoster.AEQUILIBRIUM, status, 25_000);
		assertRidersRead(AbilityRoster.ALL_IN, status, 26_000);
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

		// The Juggernaut rows ride bare windows: they draw whatever the bar did, and read
		// only their own lane's window.
		PlayerRuntime holding = new PlayerRuntime();
		assertEquals(0, AbilityRoster.OFFLOAD.riders().get(0).remaining().applyAsLong(snapshot(holding)));
		assertTrue(holding.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L));
		assertTrue(holding.arm(Lane.ONSLAUGHT, 0, OnslaughtAbility.WINDOW_TICKS * 50L));
		PlayerStatusPayload armed = snapshot(holding);
		assertEquals(OffloadAbility.WINDOW_TICKS * 50L,
				AbilityRoster.OFFLOAD.riders().get(0).remaining().applyAsLong(armed));
		assertEquals(OnslaughtAbility.WINDOW_TICKS * 50L,
				AbilityRoster.ONSLAUGHT.riders().get(0).remaining().applyAsLong(armed));
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
	private static PlayerStatusPayload windows(long offload, long onslaught, long flip, long roulette, long aeq,
			long allIn) {
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			lanes.put(lane, new LaneRead(0, 0, false));
		}
		lanes.put(Lane.OFFLOAD, new LaneRead(0, offload, false));
		lanes.put(Lane.ONSLAUGHT, new LaneRead(0, onslaught, true));
		lanes.put(Lane.ROTATE, new LaneRead(0, flip, false));
		lanes.put(Lane.ROULETTE, new LaneRead(0, roulette, false));
		lanes.put(Lane.OATH, new LaneRead(0, aeq, false));
		lanes.put(Lane.ALL_IN, new LaneRead(0, allIn, false));
		PlayerRuntime.State state = new PlayerRuntime.State(lanes, RotateAbility.Stance.SCUTUM,
				RouletteAbility.Outcome.EDGE, null, 0);
		return new PlayerStatusPayload("infusedclasses:juggernaut", state, PlayerRuntime.BarRead.NONE);
	}

	/** The snapshot a server would send for this runtime; the class does not matter to what rows read. */
	private static PlayerStatusPayload snapshot(PlayerRuntime runtime) {
		return PlayerStatusPayload.of(juggernaut(), runtime, runtime.state(0), 0);
	}
}
