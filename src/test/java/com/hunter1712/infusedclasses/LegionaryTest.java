package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legionary decisions: stance toggle, oath payoffs, and the mult matrix.
 * Adapters own the attributes and sounds; this pins what they are told.
 */
class LegionaryTest {
	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	@Test
	void registryCarriesLegionaryWithThreeActives() {
		ClassDefinition def = legionary();

		assertFalse(def.locked());
		assertEquals("Legionary", def.displayName());
		assertEquals(0xFFB87333, def.accentColor());
		assertEquals(List.of(ClassRegistry.ROTATE_ABILITY, ClassRegistry.IMPETUS_ABILITY,
				ClassRegistry.TESTUDO_ABILITY), def.activeAbilities());
		assertEquals(List.of("Rotate", "Impetus", "Testudo"),
				def.actives().stream().map(ClassDefinition.ActiveAbility::name).toList());
	}

	@Test
	void copyReflectsLiveNumbers() {
		String tooltip = ClassPresentation.tooltipFor(legionary());

		assertTrue(tooltip.contains("(R, " + (RotateAbility.ROTATE_MS / 1000) + "s)"));
		assertTrue(tooltip.contains("Announces Gladius!/Scutum!"));
		assertTrue(tooltip.contains("+" + ConditionalPassive.amount(ImpetusAbility.IMPETUS_BONUS)));
		assertTrue(tooltip.contains("x" + ImpetusAbility.IMPETUS_TAKEN_MULT));
		assertTrue(tooltip.contains("+" + ConditionalPassive.amount(TestudoAbility.TESTUDO_ARMOR)));
		assertTrue(tooltip.contains("x" + TestudoAbility.TESTUDO_DEAL_MULT));
		assertTrue(tooltip.contains("shares " + (Cooldowns.OATH_MS / 1000) + "s oath"));
		assertTrue(tooltip.contains(ImpetusAbility.CAST_WORD));
		assertTrue(tooltip.contains(TestudoAbility.CAST_WORD));
		assertFalse(tooltip.contains("Roots"));
	}

	@Test
	void rotateFiresInAnyStanceAndFlips() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(RotateAbility.Stance.SCUTUM, runtime.stance());

		RotateAbility.Result first = RotateAbility.fire(legionary(), runtime, 0);

		assertEquals(RotateAbility.Kind.FIRED, first.kind());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.stance());
		assertEquals(RotateAbility.Stance.GLADIUS, first.stance());
		assertEquals("Gladius!", first.announcement().text());
		assertEquals(Cooldowns.ROTATE_MS, runtime.rotateRemaining(0));
		// The rotate lane is its own: oath stays ready.
		assertEquals(0, runtime.oathRemaining(0));
	}

	@Test
	void rotateRejectsArmNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		RotateAbility.fire(legionary(), runtime, 0);
		RotateAbility.Result second = RotateAbility.fire(legionary(), runtime, 1_000);

		assertEquals(RotateAbility.Kind.ON_COOLDOWN, second.kind());
		assertNull(second.stance());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.stance());

		PlayerRuntime wrong = new PlayerRuntime();
		RotateAbility.Result rejected = RotateAbility.fire(null, wrong, 0);

		assertEquals(RotateAbility.Kind.WRONG_CLASS, rejected.kind());
		assertNull(rejected.announcement());
		assertEquals(0, wrong.rotateRemaining(0));
		assertEquals(RotateAbility.Stance.SCUTUM, wrong.stance());
	}

	@Test
	void stanceMatrix() {
		assertEquals(1.2, RotateAbility.takeMult(RotateAbility.Stance.GLADIUS, false));
		assertEquals(1.5, RotateAbility.takeMult(RotateAbility.Stance.GLADIUS, true));
		assertEquals(1.0, RotateAbility.takeMult(RotateAbility.Stance.SCUTUM, false));
		// Mixed states live: rotating inside a window keeps it, so the mults stack.
		assertEquals(1.25, RotateAbility.takeMult(RotateAbility.Stance.SCUTUM, true));
		assertEquals(0.8, RotateAbility.dealMult(RotateAbility.Stance.SCUTUM, false));
		assertEquals(0.56, RotateAbility.dealMult(RotateAbility.Stance.SCUTUM, true), 1e-9);
		assertEquals(1.0, RotateAbility.dealMult(RotateAbility.Stance.GLADIUS, false));
		assertEquals(0.7, RotateAbility.dealMult(RotateAbility.Stance.GLADIUS, true), 1e-9);
	}

	@Test
	void impetusFiresOnlyInGladiusAndLocksOath() {
		PlayerRuntime runtime = new PlayerRuntime();

		// Scutum default: Impetus stays silent, Testudo fires.
		assertEquals(ImpetusAbility.Kind.WRONG_CLASS,
				ImpetusAbility.fire(legionary(), runtime, 0).kind());
		TestudoAbility.Result testudo = TestudoAbility.fire(legionary(), runtime, 0);

		assertEquals(TestudoAbility.Kind.FIRED, testudo.kind());
		assertEquals(Cooldowns.OATH_MS, runtime.oathRemaining(0));
		assertTrue(runtime.isTestudoLive(0));
		// Shared lane: rotating to Gladius does not free the oath.
		assertEquals(RotateAbility.Kind.FIRED, RotateAbility.fire(legionary(), runtime, 0).kind());
		ImpetusAbility.Result locked = ImpetusAbility.fire(legionary(), runtime, 1_000);

		assertEquals(ImpetusAbility.Kind.ON_COOLDOWN, locked.kind());
		assertEquals(Cooldowns.OATH_MS - 1_000, locked.remainingMs());
	}

	@Test
	void impetusWindowLivesSixSeconds() {
		PlayerRuntime runtime = new PlayerRuntime();
		RotateAbility.fire(legionary(), runtime, 0);
		ImpetusAbility.fire(legionary(), runtime, 0);

		assertTrue(runtime.isImpetusLive(0));
		assertTrue(runtime.isImpetusLive(5_999));
		assertFalse(runtime.isImpetusLive(6_000));
		assertFalse(runtime.isTestudoLive(0));
	}

	@Test
	void wrongStanceAndClassArmNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		TestudoAbility.Result gladius = TestudoAbility.fire(legionary(), flipToGladius(), 0);

		assertEquals(TestudoAbility.Kind.WRONG_CLASS, gladius.kind());
		assertNull(gladius.announcement());
		assertEquals(0, runtime.oathRemaining(0));
		assertEquals(ImpetusAbility.Kind.WRONG_CLASS,
				ImpetusAbility.fire(null, runtime, 0).kind());
	}

	@Test
	void resetClearsWindowsLanesAndStance() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.flipStance();
		ImpetusAbility.fire(legionary(), runtime, 0);
		RotateAbility.fire(legionary(), runtime, 10_000);
		runtime.resetCooldowns();

		assertEquals(0, runtime.oathRemaining(0));
		assertEquals(0, runtime.rotateRemaining(0));
		assertFalse(runtime.isImpetusLive(0));
		assertFalse(runtime.isTestudoLive(0));
		assertEquals(RotateAbility.Stance.SCUTUM, runtime.stance());
	}

	private static PlayerRuntime flipToGladius() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.flipStance();
		return runtime;
	}
}
