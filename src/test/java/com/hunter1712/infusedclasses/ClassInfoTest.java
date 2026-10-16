package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code /class info} lines: pure, composed from the definition, the runtime and the
 * passed now. Pins the per-class lanes at ready/cooling boundaries plus the Legionary's
 * stance line, so the copy cannot drift from the lane reads.
 */
class ClassInfoTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	private static List<String> texts(ClassDefinition def, PlayerRuntime runtime, long now) {
		return ClassInfo.lines(def, runtime, now).stream().map(ChatLine::text).toList();
	}

	@Test
	void freshJuggernautShowsBothLanesReady() {
		assertEquals(List.of("Class: Juggernaut", "Rush: ready", "Rend: ready"),
				texts(juggernaut(), new PlayerRuntime(), 0));
	}

	@Test
	void coolingJuggernautPinsOneDecimal() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRush(0);
		runtime.tryUseRend(0, 0);

		assertEquals(List.of("Class: Juggernaut", "Rush: 18.4s", "Rend: 53.4s"),
				texts(juggernaut(), runtime, 6_600));
	}

	@Test
	void aLaneAtItsExpiryBoundaryReadsReady() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRush(0);

		assertEquals(List.of("Class: Juggernaut", "Rush: ready", "Rend: ready"),
				texts(juggernaut(), runtime, Cooldowns.RUSH_MS));
	}

	@Test
	void freshLegionaryShowsRotateOathAndScutum() {
		assertEquals(List.of("Class: Legionary", "Rotate: ready", "Aequilibrium: ready", "Stance: Scutum"),
				texts(legionary(), new PlayerRuntime(), 0));
	}

	@Test
	void coolingLegionaryPinsOneDecimalAndTheFlippedStance() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRotate(0);
		runtime.tryUseAequilibrium(0);
		runtime.flipStance(0, RotateAbility.Stance.GLADIUS);

		assertEquals(List.of("Class: Legionary", "Rotate: 1.4s", "Aequilibrium: 43.4s", "Stance: Gladius"),
				texts(legionary(), runtime, 6_600));
	}

	@Test
	void freshGamblerShowsBothLanesReadyAndNoStance() {
		assertEquals(List.of("Class: Gambler", "Roulette: ready", "All In: ready"),
				texts(gambler(), new PlayerRuntime(), 0));
	}

	@Test
	void coolingGamblerPinsOneDecimal() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRoulette(0, RouletteAbility.Outcome.EDGE);
		runtime.tryUseAllIn(0);

		assertEquals(List.of("Class: Gambler", "Roulette: 18.4s", "All In: 68.4s"),
				texts(gambler(), runtime, 6_600));
	}

	@Test
	void noClassYieldsOneGrayLine() {
		List<ChatLine> lines = ClassInfo.lines(null, new PlayerRuntime(), 0);

		assertEquals(List.of("No class chosen"), lines.stream().map(ChatLine::text).toList());
		assertEquals(ChatFormatting.GRAY, lines.get(0).style());
	}

	@Test
	void theClassLineIsGrayAndFirst() {
		List<ChatLine> lines = ClassInfo.lines(juggernaut(), new PlayerRuntime(), 0);

		assertEquals("Class: Juggernaut", lines.get(0).text());
		assertEquals(ChatFormatting.GRAY, lines.get(0).style());
		for (ChatLine lane : lines.subList(1, lines.size())) {
			assertNull(lane.style(), lane.text());
		}
	}

	@Test
	void infoLinesCarryNoKeyOrHealCopy() {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (String text : texts(def, new PlayerRuntime(), 0)) {
				assertFalse(text.contains("("), text);
				assertFalse(text.toLowerCase().contains("heal"), text);
			}
		}
	}

	@Test
	void onlyTheCarriedLanesAppear() {
		List<String> juggernaut = texts(juggernaut(), new PlayerRuntime(), 0);
		assertFalse(juggernaut.contains("Rotate: ready"));
		assertFalse(juggernaut.contains("Roulette: ready"));

		List<String> legionary = texts(legionary(), new PlayerRuntime(), 0);
		assertTrue(legionary.contains("Stance: Scutum"));
		assertFalse(legionary.contains("Rush: ready"));
		assertFalse(legionary.contains("Rend: ready"));

		List<String> gambler = texts(gambler(), new PlayerRuntime(), 0);
		assertFalse(gambler.contains("Stance: Scutum"));
		assertFalse(gambler.contains("Rotate: ready"));
	}

	@Test
	void aCustomDefinitionComposesByItsOwnActives() {
		ClassDefinition rendOnly = new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(), List.of(), 0, "",
				List.of(new ClassDefinition.ActiveAbility(ClassRegistry.REND_ABILITY, "Rend", "test copy")),
				List.of(),
				CritRule.universal());

		assertEquals(List.of("Class: Test", "Rend: ready"), texts(rendOnly, new PlayerRuntime(), 0));
	}
}
