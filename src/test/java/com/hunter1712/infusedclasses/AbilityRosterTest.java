package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ability roster: one row per fired ability, carrying the word, the lane read and the
 * slot binds. Pins that every registered active has a row, that rows read their own lane,
 * and that the binds land where ADR-0009 says (the ultimate keys on the ROTATE row).
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
}
