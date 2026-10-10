package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vigor: the Juggernaut's HP-scaled pair. Pure bucket math, per-attribute bonus mapping,
 * registry wiring, and picker copy — asserted without a player or attribute instance.
 */
class VigorTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static HpScaledPassive damage() {
		return juggernaut().hpPassives().get(0);
	}

	private static HpScaledPassive speed() {
		return juggernaut().hpPassives().get(1);
	}

	@Test
	void bucketsFollowCurrentHp() {
		assertEquals(10, HpScaledPassive.bucketForCurrent(20, 20));
		assertEquals(9, HpScaledPassive.bucketForCurrent(18, 20));
		assertEquals(5, HpScaledPassive.bucketForCurrent(10, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(1, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(0, 20));
	}

	@Test
	void bucketsScaleWithTheBiggerPool() {
		assertEquals(10, HpScaledPassive.bucketForCurrent(40, 40));
		assertEquals(5, HpScaledPassive.bucketForCurrent(20, 40));
		assertEquals(0, HpScaledPassive.bucketForCurrent(0, 40));
	}

	@Test
	void bucketsClampPastTheEnds() {
		assertEquals(10, HpScaledPassive.bucketForCurrent(25, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(-3, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(10, 0));
	}

	@Test
	void bucketZeroYieldsNoModifier() {
		assertNull(PassiveEffect.forBucket(damage(), 0));
		assertNull(PassiveEffect.forBucket(speed(), 0));
		assertNull(PassiveEffect.forBucket(damage(), -1));
		assertNull(PassiveEffect.forBucket(null, 5));
	}

	@Test
	void bonusIsBasePlusBucketTimesPerStep() {
		assertEquals(0.5, PassiveEffect.forBucket(damage(), 5).amount());
		assertEquals(2.0, PassiveEffect.forBucket(damage(), 10).amount());
		assertEquals(0.25, PassiveEffect.forBucket(speed(), 5).amount());
		assertEquals(0.5, PassiveEffect.forBucket(speed(), 10).amount());
	}

	@Test
	void bonusNeverExceedsTheCap() {
		assertEquals(2.0, PassiveEffect.forBucket(damage(), 11).amount());
		assertEquals(0.5, PassiveEffect.forBucket(speed(), 99).amount());
	}

	@Test
	void damageShrinksTheBonus() {
		assertEquals(5, HpScaledPassive.bucketForCurrent(10, 20));
		assertEquals(9, HpScaledPassive.bucketForCurrent(18, 20));
		assertEquals(0.5, PassiveEffect.forBucket(damage(), 5).amount());
		assertEquals(1.7, PassiveEffect.forBucket(damage(), 9).amount(), 1e-9);
	}

	@Test
	void bonusCarriesItsOwnAttributeAndId() {
		PassiveEffect.Modifier dmg = PassiveEffect.forBucket(damage(), 3);
		assertEquals(ClassRegistry.ATTACK_DAMAGE, dmg.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "vigor_damage"), dmg.modifierId());

		PassiveEffect.Modifier spd = PassiveEffect.forBucket(speed(), 3);
		assertEquals(ClassRegistry.ATTACK_SPEED, spd.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "vigor_speed"), spd.modifierId());
	}

	@Test
	void juggernautCarriesThePairWithoutTouchingMomentum() {
		List<HpScaledPassive> pair = juggernaut().hpPassives();

		assertEquals(2, pair.size());
		assertEquals("Vigor Damage", pair.get(0).name());
		assertEquals("Vigor", pair.get(0).group());
		assertEquals(0.3, pair.get(0).perStep());
		assertEquals(-1.0, pair.get(0).baseBonus());
		assertEquals("Vigor Speed", pair.get(1).name());
		assertEquals(0.05, pair.get(1).perStep());
		assertEquals(0.0, pair.get(1).baseBonus());

		ConditionalPassive momentum = juggernaut().passives().get(0);
		assertEquals(1, juggernaut().passives().size());
		assertEquals("Momentum", momentum.name());
		assertEquals(ConditionalPassive.ON_HIT_DEALT, momentum.trigger());
		assertEquals(20, momentum.maxStacks());
	}

	@Test
	void copyIsGeneratedFromTheNumbers() {
		assertEquals("HP-scaled: -1 at 0 HP to +2 full HP.",
				damage().fullDesc());
		assertEquals("HP-scaled: 0 at 0 HP to +0.5 full HP.",
				speed().fullDesc());
	}

	@Test
	void cardShowsDetailLinesWhileVigorStaysGrouped() {
		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(juggernaut());
		List<String> texts = lines.stream().map(ClassPresentation.CardLine::text).toList();

		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Momentum \u2014 ")));
		assertTrue(texts.stream().noneMatch(t -> t.equals("Vigor Damage") || t.equals("Vigor Speed")));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Vigor \u2014 ")
				&& t.contains(damage().fullDesc()) && t.contains(speed().fullDesc())));
	}

	@Test
	void rendStillSpendsMomentumOnly() {
		assertEquals(juggernaut().passives().get(0),
				juggernaut().passiveForTrigger(ConditionalPassive.ON_HIT_DEALT));

		// The pair rides along without entering the stacks pipeline: hits still earn
		// Momentum alone, so the spender's consume can never touch Vigor.
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);

		assertEquals(2, runtime.stacks(juggernaut().passives().get(0)));
	}
}
