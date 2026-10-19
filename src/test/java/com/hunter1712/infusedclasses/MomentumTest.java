package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Momentum bar as a decision: the three checkpoints at 15/30/45, the reserve above
 * them, the knockback resistance on every row and the modifiers each row writes. Plain
 * JUnit: no player, level or attribute instance is constructed.
 */
class MomentumTest {
	private static List<String> ids(List<PassiveEffect.Modifier> modifiers) {
		return modifiers.stream()
				.map(PassiveEffect.Modifier::modifierId)
				.map(Identifier::toString)
				.toList();
	}

	private static List<Double> amounts(List<PassiveEffect.Modifier> modifiers) {
		return modifiers.stream().map(PassiveEffect.Modifier::amount).toList();
	}

	@Test
	void theBarIsTheThreeCheckpointsAndA60Ceiling() {
		assertEquals(60, Momentum.MAX_STACKS);
		assertEquals(5, Momentum.GRACE_SECONDS);
		assertEquals(List.of(15, 30, 45),
				Momentum.TIERS.stream().map(ConditionalPassive.Tier::threshold).toList());
		assertEquals(List.of("Steady", "Braced", "Rooted"),
				Momentum.TIERS.stream().map(ConditionalPassive.Tier::name).toList());
	}

	@Test
	void theBarReadsTheLastRowItHasReached() {
		ConditionalPassive momentum = Momentum.PASSIVE;

		assertEquals("", momentum.tierNameFor(0));
		assertEquals("", momentum.tierNameFor(14));
		assertEquals("Steady", momentum.tierNameFor(15));
		assertEquals("Steady", momentum.tierNameFor(29));
		assertEquals("Braced", momentum.tierNameFor(30));
		assertEquals("Braced", momentum.tierNameFor(44));
		assertEquals("Rooted", momentum.tierNameFor(45));
		assertEquals("Rooted", momentum.tierNameFor(60));
	}

	@Test
	void belowTheFirstRowWantsNothing() {
		assertEquals(List.of(), Momentum.forStacks(0));
		assertEquals(List.of(), Momentum.forStacks(14));
	}

	@Test
	void eachRowWritesArmorWeightAndKnockback() {
		List<PassiveEffect.Modifier> steady = Momentum.forStacks(15);
		assertEquals(List.of("infusedclasses:momentum_armor", "infusedclasses:momentum_weight",
				"infusedclasses:momentum_knockback"), ids(steady));
		assertEquals(List.of(ClassRegistry.ARMOR, ClassRegistry.MOVEMENT_SPEED,
				ClassRegistry.KNOCKBACK_RESISTANCE),
				steady.stream().map(PassiveEffect.Modifier::attribute).toList());
		assertEquals(List.of(1.0, -0.005, 0.25), amounts(steady));

		assertEquals(List.of(3.0, -0.01, 0.5), amounts(Momentum.forStacks(30)));
		assertEquals(List.of(5.0, -0.015, 1.0), amounts(Momentum.forStacks(45)));
	}

	@Test
	void theHeadroomAboveTheTopRowStaysRooted() {
		assertEquals(Momentum.forStacks(45), Momentum.forStacks(60));
	}

	@Test
	void theSweepSetHoldsEveryIdOnce() {
		List<PassiveEffect.Modifier> all = Momentum.allModifiers();

		assertEquals(List.of("infusedclasses:momentum_armor", "infusedclasses:momentum_weight",
				"infusedclasses:momentum_knockback"), ids(all));
		assertEquals(all.size(),
				all.stream().map(PassiveEffect.Modifier::modifierId).distinct().count());
	}

	@Test
	void theBarCarriesItsOwnRows() {
		assertEquals(Momentum.allModifiers(), Momentum.PASSIVE.barRows().apply(Momentum.MAX_STACKS));
		assertEquals(Momentum.forStacks(30), Momentum.PASSIVE.barRows().apply(30));
		assertEquals(List.of(), Momentum.PASSIVE.barRows().apply(0));
	}

	@Test
	void theOnePassiveIsFedByBothHitDirections() {
		ConditionalPassive momentum = Momentum.PASSIVE;

		assertEquals("Momentum", momentum.name());
		assertEquals(2, momentum.earns().size());
		assertEquals(Trigger.ON_HIT_DEALT, momentum.earns().get(0).trigger());
		assertEquals(1, momentum.earns().get(0).stacks());
		assertEquals(Trigger.ON_HIT_TAKEN, momentum.earns().get(1).trigger());
		assertEquals(1, momentum.earns().get(1).stacks());
		assertEquals(60, momentum.maxStacks());
		assertEquals(5, momentum.graceSeconds());
	}

	@Test
	void copySpellsTheEarnsTheRowsAndTheRate() {
		assertEquals("Land a hit: +1. Take a hit: +1. Max 60. 5s no feed: -1/s. "
				+ "15 Steady: +1 armor, -0.005 speed, +0.25 knockback resistance. "
				+ "30 Braced: +3 armor, -0.01 speed, +0.5 knockback resistance. "
				+ "45 Rooted: +5 armor, -0.015 speed, +1 knockback resistance.",
				Momentum.PASSIVE.fullDesc());
	}
}
