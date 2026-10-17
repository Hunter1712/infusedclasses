package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legionary decisions: stance toggle, flip buffs, the oath ultimate, and the mult matrix.
 * Adapters own the attributes and sounds; this pins what they are told.
 */
class LegionaryTest {
	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	@Test
	void registryCarriesLegionaryWithTwoActivesAndTwoFlipBuffs() {
		ClassDefinition def = legionary();

		assertEquals("Legionary", def.displayName());
		assertEquals(0xFFB87333, def.accentColor());
		assertEquals(List.of(ClassRegistry.ROTATE_ABILITY, ClassRegistry.AEQUILIBRIUM_ABILITY),
				def.actives().stream().map(ClassDefinition.ActiveAbility::id).toList());
		assertEquals(List.of("Rotate", "Aequilibrium"),
				def.actives().stream().map(ClassDefinition.ActiveAbility::name).toList());
		assertEquals(List.of("Impetus", "Testudo"),
				def.flipBuffs().stream().map(ClassDefinition.FlipBuff::name).toList());
	}

	@Test
	void copyReflectsLiveNumbers() {
		List<String> lines = ClassPresentation.cardLinesFor(legionary()).stream()
				.map(ClassPresentation.CardLine::text).toList();
		String copy = String.join("\n", lines);

		assertTrue(copy.contains("(R, " + (Cooldowns.ROTATE_MS / 1000) + "s)"));
		assertTrue(copy.contains("Flip to Gladius"));
		assertTrue(copy.contains("Flip to Scutum"));
		assertTrue(copy.contains("next hit x" + RotateAbility.SWITCH_DEAL_MULT));
		assertTrue(copy.contains("take x" + RotateAbility.SWITCH_TAKE_MULT));
		assertTrue(copy.contains("+" + ConditionalPassive.amount(AequilibriumAbility.EDGE_BONUS) + " damage"));
		assertTrue(copy.contains("+" + ConditionalPassive.amount(AequilibriumAbility.PLATE_BONUS) + " armor"));
		assertTrue(copy.contains("(V, " + (Cooldowns.OATH_MS / 1000) + "s oath)"));
		assertTrue(copy.contains("No downside"));
		assertFalse(copy.contains("knockback"));
		assertFalse(copy.contains("Announces"));
		assertFalse(copy.contains("Whoosh"));
		assertFalse(copy.contains("Smash"));
	}

	@Test
	void rotateFiresInAnyStanceAndFlips() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(RotateAbility.Stance.SCUTUM, runtime.stance());

		RotateAbility.Result first = RotateAbility.fire(legionary(), runtime, 0);

		assertTrue(first.outcome().fired());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.stance());
		assertEquals(RotateAbility.Stance.GLADIUS, first.stance());
		assertEquals("Gladius!", first.outcome().announcement().text());
		assertEquals(Cooldowns.ROTATE_MS, runtime.rotateRemaining(0));
		// The rotate lane is its own: oath stays ready.
		assertEquals(0, runtime.oathRemaining(0));
	}

	@Test
	void rotateRejectsArmNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		RotateAbility.fire(legionary(), runtime, 0);
		RotateAbility.Result second = RotateAbility.fire(legionary(), runtime, 1_000);

		assertFalse(second.outcome().fired());
		assertNull(second.stance());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.stance());

		PlayerRuntime wrong = new PlayerRuntime();
		RotateAbility.Result rejected = RotateAbility.fire(null, wrong, 0);

		assertFalse(rejected.outcome().fired());
		assertNull(rejected.outcome().announcement());
		assertEquals(0, wrong.rotateRemaining(0));
		assertEquals(RotateAbility.Stance.SCUTUM, wrong.stance());
	}

	@Test
	void stanceMatrix() {
		// Stance-only mults (RotateAbility)
		assertEquals(1.1, RotateAbility.takeMult(RotateAbility.Stance.GLADIUS));
		assertEquals(1.0, RotateAbility.takeMult(RotateAbility.Stance.SCUTUM));
		assertEquals(0.9, RotateAbility.dealMult(RotateAbility.Stance.SCUTUM));
		assertEquals(1.0, RotateAbility.dealMult(RotateAbility.Stance.GLADIUS));
		// Flip factors ride the same constants as the picker copy, so balance and copy cannot drift.
		assertEquals(1.10, RotateAbility.SWITCH_DEAL_MULT);
		assertEquals(0.85, RotateAbility.SWITCH_TAKE_MULT);
	}

	@Test
	void flipToGladiusArmsImpetusOnceAndExpires() {
		PlayerRuntime runtime = new PlayerRuntime();
		RotateAbility.fire(legionary(), runtime, 0);

		assertTrue(runtime.hasImpetusPending(0));
		assertTrue(runtime.hasImpetusPending(RotateAbility.SWITCH_DEAL_MS - 1));
		assertFalse(runtime.hasImpetusPending(RotateAbility.SWITCH_DEAL_MS));
		assertTrue(runtime.consumeImpetusBonus(0));
		assertFalse(runtime.hasImpetusPending(0));
		assertFalse(runtime.consumeImpetusBonus(0));
	}

	@Test
	void flipToScutumArmsTestudoCatchAndExpires() {
		PlayerRuntime runtime = new PlayerRuntime();
		// No flip yet: no catch, even in the default stance.
		assertFalse(runtime.isTestudoBuffLive(0));
		runtime.flipStance(0, RotateAbility.Stance.SCUTUM);

		assertTrue(runtime.isTestudoBuffLive(0));
		assertTrue(runtime.isTestudoBuffLive(RotateAbility.SWITCH_TAKE_MS - 1));
		assertFalse(runtime.isTestudoBuffLive(RotateAbility.SWITCH_TAKE_MS));
	}

	@Test
	void flippingAwayDropsTheImpetusBuff() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.flipStance(0, RotateAbility.Stance.GLADIUS);
		assertTrue(runtime.hasImpetusPending(0));

		runtime.flipStance(1_000, RotateAbility.Stance.SCUTUM);

		assertFalse(runtime.hasImpetusPending(1_000));
		assertTrue(runtime.isTestudoBuffLive(1_000));
	}

	@Test
	void aequilibriumFiresInEitherStanceAndLocksOath() {
		PlayerRuntime scutum = new PlayerRuntime();
		AbilityOutcome first = AequilibriumAbility.fire(legionary(), scutum, 0);

		assertTrue(first.fired());
		assertEquals("Aequilibrium!", first.announcement().text());
		assertEquals(Cooldowns.OATH_MS, scutum.oathRemaining(0));
		assertTrue(scutum.isAequilibriumLive(0));

		PlayerRuntime gladius = new PlayerRuntime();
		gladius.flipStance(0, RotateAbility.Stance.GLADIUS);
		assertTrue(AequilibriumAbility.fire(legionary(), gladius, 0).fired());

		AbilityOutcome onCooldown = AequilibriumAbility.fire(legionary(), scutum, 1_000);
		assertFalse(onCooldown.fired());
		assertEquals(Cooldowns.OATH_MS - 1_000, onCooldown.remainingMs());
	}

	@Test
	void aequilibriumWindowLivesSixSeconds() {
		PlayerRuntime runtime = new PlayerRuntime();
		AequilibriumAbility.fire(legionary(), runtime, 0);

		assertTrue(runtime.isAequilibriumLive(0));
		assertTrue(runtime.isAequilibriumLive(5_999));
		assertFalse(runtime.isAequilibriumLive(6_000));
	}

	@Test
	void wrongClassArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();

		assertFalse(AequilibriumAbility.fire(ClassRegistry.byId(ClassRegistry.JUGGERNAUT), runtime, 0).fired());
		assertFalse(AequilibriumAbility.fire(null, runtime, 0).fired());
		assertEquals(0, runtime.oathRemaining(0));
		assertFalse(runtime.isAequilibriumLive(0));
	}

	@Test
	void resetClearsFlipWindowsLanesAndStance() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.flipStance(0, RotateAbility.Stance.GLADIUS);
		AequilibriumAbility.fire(legionary(), runtime, 0);
		RotateAbility.fire(legionary(), runtime, 10_000);
		runtime.resetCooldowns();

		assertEquals(0, runtime.oathRemaining(0));
		assertEquals(0, runtime.rotateRemaining(0));
		assertFalse(runtime.hasImpetusPending(0));
		assertFalse(runtime.isTestudoBuffLive(0));
		assertFalse(runtime.isAequilibriumLive(0));
		assertEquals(RotateAbility.Stance.SCUTUM, runtime.stance());
	}

	@Test
	void stanceAurasAreSingleSourced() {
		var gladius = RotateAbility.auraFor(RotateAbility.Stance.GLADIUS);
		var scutum = RotateAbility.auraFor(RotateAbility.Stance.SCUTUM);
		var window = AequilibriumAbility.windowModifiers();

		// What the applier writes is what it removes: two auras plus the window pair.
		assertEquals(
				java.util.List.of(RotateAbility.GLADIUS_DAMAGE_ID, RotateAbility.GLADIUS_ALACRITY_ID),
				gladius.stream().map(PassiveEffect.Modifier::modifierId).toList());
		assertEquals(
				java.util.List.of(RotateAbility.SCUTUM_PLATE_ID, RotateAbility.SCUTUM_TOUGHNESS_ID,
						RotateAbility.SCUTUM_WEIGHT_ID),
				scutum.stream().map(PassiveEffect.Modifier::modifierId).toList());
		assertEquals(
				java.util.List.of(AequilibriumAbility.EDGE_ID, AequilibriumAbility.PLATE_ID),
				window.stream().map(PassiveEffect.Modifier::modifierId).toList());
		assertEquals(1.0, gladius.get(0).amount());
		assertEquals(0.3, gladius.get(1).amount());
		assertEquals(3.0, scutum.get(0).amount());
		assertEquals(-0.006, scutum.get(2).amount());
		assertEquals(AequilibriumAbility.EDGE_BONUS, window.get(0).amount());
		assertEquals(AequilibriumAbility.PLATE_BONUS, window.get(1).amount());
	}
}
