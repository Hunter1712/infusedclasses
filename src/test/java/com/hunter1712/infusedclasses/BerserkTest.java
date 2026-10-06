package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Berserk: the Warrior's HP-scaled pair. Pure bucket math, per-attribute bonus mapping,
 * registry wiring, and picker copy — asserted without a player or attribute instance.
 */
class BerserkTest {
	private static ClassDefinition warrior() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR);
	}

	private static HpScaledPassive damage() {
		return warrior().hpPassives().get(0);
	}

	private static HpScaledPassive speed() {
		return warrior().hpPassives().get(1);
	}

	@Test
	void bucketsFollowMissingHp() {
		assertEquals(0, HpScaledPassive.bucket(20, 20));
		assertEquals(1, HpScaledPassive.bucket(18, 20));
		assertEquals(5, HpScaledPassive.bucket(10, 20));
		assertEquals(9, HpScaledPassive.bucket(1, 20));
		assertEquals(10, HpScaledPassive.bucket(0, 20));
	}

	@Test
	void bucketsScaleWithTheBiggerPool() {
		assertEquals(0, HpScaledPassive.bucket(40, 40));
		assertEquals(5, HpScaledPassive.bucket(20, 40));
		assertEquals(10, HpScaledPassive.bucket(0, 40));
	}

	@Test
	void bucketsClampPastTheEnds() {
		assertEquals(0, HpScaledPassive.bucket(25, 20));
		assertEquals(10, HpScaledPassive.bucket(-3, 20));
		assertEquals(0, HpScaledPassive.bucket(10, 0));
	}

	@Test
	void bucketZeroYieldsNoModifier() {
		assertNull(PassiveEffect.forBucket(damage(), 0));
		assertNull(PassiveEffect.forBucket(speed(), 0));
		assertNull(PassiveEffect.forBucket(damage(), -1));
		assertNull(PassiveEffect.forBucket(null, 5));
	}

	@Test
	void bonusIsBucketTimesPerStep() {
		assertEquals(1.5, PassiveEffect.forBucket(damage(), 5).amount());
		assertEquals(3.0, PassiveEffect.forBucket(damage(), 10).amount());
		assertEquals(0.25, PassiveEffect.forBucket(speed(), 5).amount());
		assertEquals(0.5, PassiveEffect.forBucket(speed(), 10).amount());
	}

	@Test
	void bonusNeverExceedsTheCap() {
		assertEquals(3.0, PassiveEffect.forBucket(damage(), 11).amount());
		assertEquals(0.5, PassiveEffect.forBucket(speed(), 99).amount());
	}

	@Test
	void healingShrinksTheBonus() {
		assertEquals(5, HpScaledPassive.bucket(10, 20));
		assertEquals(1, HpScaledPassive.bucket(18, 20));
		assertEquals(1.5, PassiveEffect.forBucket(damage(), 5).amount());
		assertEquals(0.3, PassiveEffect.forBucket(damage(), 1).amount());
	}

	@Test
	void bonusCarriesItsOwnAttributeAndId() {
		PassiveEffect.Modifier dmg = PassiveEffect.forBucket(damage(), 3);
		assertEquals(ClassRegistry.ATTACK_DAMAGE, dmg.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "berserk_damage"), dmg.modifierId());

		PassiveEffect.Modifier spd = PassiveEffect.forBucket(speed(), 3);
		assertEquals(ClassRegistry.ATTACK_SPEED, spd.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "berserk_speed"), spd.modifierId());
	}

	@Test
	void warriorCarriesThePairWithoutTouchingBulwark() {
		List<HpScaledPassive> pair = warrior().hpPassives();

		assertEquals(2, pair.size());
		assertEquals("Berserk Damage", pair.get(0).name());
		assertEquals("Berserk", pair.get(0).group());
		assertEquals(0.3, pair.get(0).perStep());
		assertEquals("Berserk Speed", pair.get(1).name());
		assertEquals(0.05, pair.get(1).perStep());

		ConditionalPassive bulwark = warrior().passives().get(0);
		assertEquals(1, warrior().passives().size());
		assertEquals("Bulwark", bulwark.name());
		assertEquals(ConditionalPassive.ON_HIT_TAKEN, bulwark.trigger());
		assertEquals(20, bulwark.maxStacks());
	}

	@Test
	void copyIsGeneratedFromTheNumbers() {
		assertEquals("Each 10% missing HP grants +0.3 attack damage (max +3 at 0 HP).",
				damage().fullDesc());
		assertEquals("Each 10% missing HP grants +0.05 attack speed (max +0.5 at 0 HP).",
				speed().fullDesc());
	}

	@Test
	void cardShowsNamesWhileTooltipShowsFullForm() {
		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(warrior());
		List<String> names = lines.stream().map(ClassPresentation.CardLine::text).toList();

		assertTrue(names.contains("Berserk"));
		assertFalse(names.contains("Berserk Damage"));
		assertFalse(names.contains("Berserk Speed"));

		String tooltip = ClassPresentation.tooltipFor(warrior());
		assertTrue(tooltip.contains("Passive: Berserk \u2014 " + damage().fullDesc() + " " + speed().fullDesc()));
	}

	@Test
	void secondWindStillSpendsBulwarkOnly() {
		assertEquals(warrior().passives().get(0), SecondWindAbility.bulwarkOf(warrior()));

		// The pair rides along without entering the stacks pipeline: hits still earn
		// Bulwark alone, so the spender's consume can never touch Berserk.
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, 0);
		runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, 0);

		assertEquals(2, runtime.stacks(warrior().passives().get(0)));
	}
}
