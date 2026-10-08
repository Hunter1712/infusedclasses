package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Universal charged rule: full swings pay full bonus, half pays a quarter,
 * spam pays nothing. Base weapon damage never routes here.
 */
class BonusCurveTest {
	@Test
	void fullHalfAndSpam() {
		assertEquals(6.0F, BonusCurve.bonusFor(6.0F, 1.0F));
		assertEquals(1.5F, BonusCurve.bonusFor(6.0F, 0.5F));
		assertEquals(0.0F, BonusCurve.bonusFor(6.0F, 0.0F));
	}

	@Test
	void impetusBonusAtQuarterOnHalfCharge() {
		assertEquals(3.0F, ImpetusAbility.bonusFor(1.0F));
		assertEquals(0.75F, ImpetusAbility.bonusFor(0.5F));
		assertEquals(0.0F, ImpetusAbility.bonusFor(0.0F));
	}

	@Test
	void rendBonusAtQuarterOnHalfCharge() {
		float full = RendAbility.empowerFor(10);

		assertEquals(16.0F, full);
		assertEquals(4.0F, BonusCurve.bonusFor(full, 0.5F));
		assertEquals(16.0F, BonusCurve.bonusFor(full, 1.0F));
	}

	@Test
	void overchargeClampsAndDeadBaseReadsZero() {
		assertEquals(6.0F, BonusCurve.bonusFor(6.0F, 1.5F));
		assertEquals(0.0F, BonusCurve.bonusFor(6.0F, -1.0F));
		assertEquals(0.0F, BonusCurve.bonusFor(0.0F, 1.0F));
		assertEquals(0.0F, BonusCurve.bonusFor(-3.0F, 1.0F));
	}

	@Test
	void fullSwingIsSingleSharedGate() {
		assertTrue(BonusCurve.isFullSwing(1.0F));
		assertFalse(BonusCurve.isFullSwing(0.9999F));
		assertFalse(BonusCurve.isFullSwing(0.5F));
	}

	@Test
	void spamFeedsZeroStacksFullFeedsOne() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
		PlayerRuntime spam = new PlayerRuntime();
		spam.onTrigger(juggernaut, ConditionalPassive.ON_HIT_DEALT, 0, 0.4F);

		assertEquals(0, spam.stacks(juggernaut.passives().get(0)));

		PlayerRuntime timed = new PlayerRuntime();
		timed.onTrigger(juggernaut, ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);

		assertEquals(1, timed.stacks(juggernaut.passives().get(0)));
	}

	@Test
	void fullSwingRestampsGrace() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.onTrigger(juggernaut, ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);
		runtime.onTrigger(juggernaut, ConditionalPassive.ON_HIT_DEALT, 3_000, 0.5F);

		// The spam swing earned nothing and moved no grace: 5s past the full
		// swing every stack drops at once.
		assertEquals(1, runtime.stacks(juggernaut.passives().get(0)));
		assertEquals(1, runtime.tickDecayMoved(juggernaut, 6_001).size());
		assertEquals(0, runtime.stacks(juggernaut.passives().get(0)));
	}
}
