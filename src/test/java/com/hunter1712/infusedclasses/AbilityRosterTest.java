package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ability roster: one row per fired ability, carrying the word, the lane read, the
 * stack's view and the slot binds. Pins that every registered active has a row, that rows
 * read their own lane and windows, and that the binds land where ADR-0009 says (the
 * ultimate keys on the ROTATE row).
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
	}

	@Test
	void rowsReadTheirOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRush(0);
		runtime.tryUseRend(0, 1);
		runtime.tryUseRotate(0, RotateAbility.Stance.GLADIUS);
		runtime.tryUseAequilibrium(0);
		runtime.tryUseRoulette(0, RouletteAbility.Outcome.EDGE);
		runtime.tryUseAllIn(0);
		PlayerRuntime.State state = runtime.state(0);

		assertEquals(Cooldowns.RUSH_MS, AbilityRoster.RUSH.laneRemaining().applyAsLong(state));
		assertEquals(Cooldowns.REND_MS, AbilityRoster.REND.laneRemaining().applyAsLong(state));
		assertEquals(Cooldowns.ROTATE_MS, AbilityRoster.ROTATE.laneRemaining().applyAsLong(state));
		assertEquals(Cooldowns.OATH_MS, AbilityRoster.AEQUILIBRIUM.laneRemaining().applyAsLong(state));
		assertEquals(Cooldowns.ROULETTE_MS, AbilityRoster.ROULETTE.laneRemaining().applyAsLong(state));
		assertEquals(Cooldowns.ALL_IN_MS, AbilityRoster.ALL_IN.laneRemaining().applyAsLong(state));
	}

	@Test
	void bindsRideTheRowsTheLegionaryMarkerOwns() {
		assertEquals(BindMorph.SignatureTarget.RUSH, AbilityRoster.RUSH.signature());
		assertEquals(BindMorph.UltimateTarget.REND, AbilityRoster.REND.ultimate());
		assertEquals(BindMorph.SignatureTarget.ROTATE, AbilityRoster.ROTATE.signature());
		assertEquals(BindMorph.UltimateTarget.AEQUILIBRIUM, AbilityRoster.ROTATE.ultimate(),
				"the ultimate keys on the ROTATE row (ADR-0009)");
		assertEquals(BindMorph.SignatureTarget.ROULETTE, AbilityRoster.ROULETTE.signature());
		assertEquals(BindMorph.UltimateTarget.ALL_IN, AbilityRoster.ALL_IN.ultimate());

		assertNull(AbilityRoster.AEQUILIBRIUM.signature());
		assertNull(AbilityRoster.AEQUILIBRIUM.ultimate());
		assertNull(AbilityRoster.RUSH.ultimate());
		assertNull(AbilityRoster.REND.signature());
	}

	@Test
	void unknownIdsFindNoRow() {
		assertNull(AbilityRoster.byAbility(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "mystery")));
	}

	@Test
	void laneReadsAgreeWithTheRuntime() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRush(0);
		runtime.tryUseRend(0, 1);
		runtime.tryUseRotate(0, RotateAbility.Stance.GLADIUS);
		runtime.tryUseAequilibrium(0);
		runtime.tryUseRoulette(1_000, RouletteAbility.Outcome.EDGE);
		runtime.tryUseAllIn(0);
		long now = 1_000;
		PlayerRuntime.State state = runtime.state(now);
		PlayerStatusPayload status = PlayerStatusPayload.of(
				ClassRegistry.byId(ClassRegistry.JUGGERNAUT), runtime, now);

		for (AbilityRoster.Row row : AbilityRoster.all()) {
			long fromPayload = row.view().laneRemaining().applyAsLong(status);
			assertTrue(fromPayload > 0, row.name());
			assertEquals(row.laneRemaining().applyAsLong(state), fromPayload, row.name());
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
		assertFalse(row.view().riders().isEmpty(), row.name());
		for (AbilityRoster.Rider rider : row.view().riders()) {
			assertEquals(window, rider.remaining().applyAsLong(status), row.name());
		}
	}

	@Test
	void riderRulesFollowTheirFlags() {
		// The Rotate riders split by what the flip landed on: Impetus when it is pending,
		// the Testudo catch while the window runs in scutum.
		PlayerRuntime gladius = new PlayerRuntime();
		gladius.tryUseRotate(0, RotateAbility.Stance.GLADIUS);
		PlayerStatusPayload flippedToGladius = snapshot(gladius);
		assertTrue(AbilityRoster.ROTATE.view().riders().get(0).wanted().test(flippedToGladius));
		assertFalse(AbilityRoster.ROTATE.view().riders().get(1).wanted().test(flippedToGladius));

		PlayerRuntime scutum = new PlayerRuntime();
		scutum.tryUseRotate(0, RotateAbility.Stance.SCUTUM);
		PlayerStatusPayload flippedToScutum = snapshot(scutum);
		assertFalse(AbilityRoster.ROTATE.view().riders().get(0).wanted().test(flippedToScutum));
		assertTrue(AbilityRoster.ROTATE.view().riders().get(1).wanted().test(flippedToScutum));

		// Zero is the dead band and draws nothing; a live band draws its own word.
		PlayerRuntime zero = new PlayerRuntime();
		zero.tryUseRoulette(0, RouletteAbility.Outcome.ZERO);
		assertFalse(AbilityRoster.ROULETTE.view().riders().get(0).wanted().test(snapshot(zero)));

		PlayerRuntime edge = new PlayerRuntime();
		edge.tryUseRoulette(0, RouletteAbility.Outcome.EDGE);
		PlayerStatusPayload spun = snapshot(edge);
		assertTrue(AbilityRoster.ROULETTE.view().riders().get(0).wanted().test(spun));
		assertEquals("Edge", AbilityRoster.ROULETTE.view().riders().get(0).label().apply(spun));

		// The Rend rider exists exactly while its next-hit snapshot is unspent.
		assertFalse(AbilityRoster.REND.view().riders().get(0).wanted().test(snapshot(new PlayerRuntime())));
		PlayerRuntime holding = new PlayerRuntime();
		holding.tryUseRend(0, 3);
		assertTrue(AbilityRoster.REND.view().riders().get(0).wanted().test(snapshot(holding)));
	}

	/** A snapshot with only the six windows set, all distinct, so a rider reading the wrong one fails. */
	private static PlayerStatusPayload windows(long rush, long rend, long flip, long roulette, long aeq, long allIn) {
		return new PlayerStatusPayload("infusedclasses:juggernaut",
				0, 0, 0, 0, 0, 0,
				rush, false,
				rend, true,
				flip, RotateAbility.Stance.SCUTUM, false,
				roulette, RouletteAbility.Outcome.EDGE,
				aeq, allIn,
				0, 0, 0, null, 0);
	}

	/** The snapshot a server would send for this runtime; the class does not matter to what rows read. */
	private static PlayerStatusPayload snapshot(PlayerRuntime runtime) {
		return PlayerStatusPayload.of(ClassRegistry.byId(ClassRegistry.JUGGERNAUT), runtime, 0);
	}
}
