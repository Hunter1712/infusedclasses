package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HUD-row decision: Roman edges, text, countdown math, hidden flag and ordering. */
class PassiveHudTest {
	@Test
	void stacksMapToAmplifierTitle() {
		assertEquals("Bulwark I", PassiveHud.rowFor(bulwark(), 1, 5_000).title());
		assertEquals("Bulwark IV", PassiveHud.rowFor(bulwark(), 4, 5_000).title());
		assertEquals("Bulwark IX", PassiveHud.rowFor(bulwark(), 9, 5_000).title());
		assertEquals("Bulwark X", PassiveHud.rowFor(bulwark(), 10, 5_000).title());
	}

	@Test
	void sublineIsWholeNumberArmorAndIconIsResistance() {
		PassiveHud.Row row = PassiveHud.rowFor(bulwark(), 3, 5_000);
		assertEquals("+3 armor", row.subline());
		assertEquals("minecraft:resistance", row.iconId());
		assertFalse(row.hidden());
	}

	@Test
	void countdownIsTheGraceLeft() {
		// Fresh hit: full 5s grace, full bar.
		PassiveHud.Row start = PassiveHud.rowFor(bulwark(), 3, 5_000);
		assertEquals(5, start.secondsLeft());
		assertEquals(1f, start.barFraction(), 0.001f);

		// 2s elapsed: 3s left, bar drained the same grace.
		PassiveHud.Row mid = PassiveHud.rowFor(bulwark(), 3, 3_000);
		assertEquals(3, mid.secondsLeft());
		assertEquals(3f / 5f, mid.barFraction(), 0.001f);

		// Grace over: zero, never negative, never recounted.
		PassiveHud.Row end = PassiveHud.rowFor(bulwark(), 3, -2_000);
		assertEquals(0, end.secondsLeft());
		assertEquals(0f, end.barFraction(), 0.001f);
	}

	@Test
	void zeroStacksAndMissingPassiveAreHidden() {
		assertTrue(PassiveHud.rowFor(bulwark(), 0, 0).hidden());
		assertTrue(PassiveHud.rowFor(null, 3, 5_000).hidden());
	}

	@Test
	void onlyPositiveStacksInRegistryOrder() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"),
				"Test", false, List.of(bulwark(), swift), null, 0, "", "", "");
		assertEquals(List.of(swift),
				PassiveHud.visiblePassives(def, p -> p.name().equals("Swift") ? 2 : 0));
		assertEquals(List.of(bulwark()),
				PassiveHud.visibleInRegistryOrder(p -> p.name().equals("Bulwark") ? 1 : 0));
		assertTrue(PassiveHud.visibleInRegistryOrder(p -> 0).isEmpty());
	}

	private static ConditionalPassive bulwark() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR).passives().get(0);
	}
}
