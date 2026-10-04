package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conditional-passive stacks: the transient half of per-player state. Time is passed in rather
 * than read, so decay and the grace period are asserted without waiting, and the class is passed
 * in rather than looked up, so this module holds no reference to a player's choice.
 */
class ConditionalPassiveStacksTest {
	@Test
	void hitTakenIncrementsStacksWhileWarriorSelected() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(warrior(), "on_hit_taken", base);
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		assertEquals(2, runtime.stacks("Bulwark"));
	}

	@Test
	void stacksCapAtMaxStacks() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		for (int i = 0; i < 15; i++) {
			runtime.onTrigger(warrior(), "on_hit_taken", base);
		}

		assertEquals(10, runtime.stacks("Bulwark"));
	}

	@Test
	void unknownTriggerAndMissingClassAreInert() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(warrior(), "on_death", base);

		assertEquals(0, runtime.stacks("Bulwark"));

		PlayerRuntime noClass = new PlayerRuntime();
		noClass.onTrigger(null, "on_hit_taken", base);

		assertEquals(0, noClass.stacks("Bulwark"));
	}

	@Test
	void stacksAreTransientNotPersistedOrCopied() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.WARRIOR);
		PlayerRuntime oldRuntime = new PlayerRuntime();
		oldRuntime.onTrigger(warrior(), "on_hit_taken", System.currentTimeMillis());

		assertEquals(ClassRegistry.WARRIOR.toString(), oldClasses.writeId());

		PlayerClasses newClasses = new PlayerClasses();
		newClasses.copyFrom(oldClasses);
		PlayerRuntime newRuntime = new PlayerRuntime();

		assertEquals(0, newRuntime.stacks("Bulwark"));
	}

	@Test
	void noDecayWithinGracePeriod() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		// Tick at 4 seconds after trigger - still in grace period
		boolean changed = runtime.tickDecay(warrior(), base + 4_000);

		assertFalse(changed);
		assertEquals(1, runtime.stacks("Bulwark"));
	}

	@Test
	void decayStartsAfterGracePeriod() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		// Tick at 6 seconds after trigger - 1 second past grace, should lose 1
		boolean changed = runtime.tickDecay(warrior(), base + 6_000);

		assertTrue(changed);
		assertEquals(0, runtime.stacks("Bulwark"));
	}

	@Test
	void decayMultipleStacksAfterExtendedTime() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(warrior(), "on_hit_taken", base);
		}
		assertEquals(5, runtime.stacks("Bulwark"));

		// Tick at 10 seconds after trigger - 5 seconds past grace, should lose 5
		boolean changed = runtime.tickDecay(warrior(), base + 10_000);

		assertTrue(changed);
		assertEquals(0, runtime.stacks("Bulwark"));
	}

	@Test
	void newTriggerResetsGraceTimer() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		// Advance 4 seconds, then trigger again
		runtime.tickDecay(warrior(), base + 4_000);
		runtime.onTrigger(warrior(), "on_hit_taken", base + 4_000); // should reset grace

		// Now advance 4 more seconds (8 total from first hit, 4 from second)
		// Since second hit reset grace, still in grace period
		runtime.tickDecay(warrior(), base + 8_000);

		assertEquals(2, runtime.stacks("Bulwark"));
	}

	@Test
	void capGraceAndDecayRateComeFromThePassiveNotFromThisModule() {
		ClassDefinition def = withPassives(new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2, 2));
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		for (int i = 0; i < 6; i++) {
			runtime.onTrigger(def, "on_land", base);
		}
		assertEquals(4, runtime.stacks("Swift")); // its own cap, not the ten Bulwark allows

		assertFalse(runtime.tickDecay(def, base + 2_000)); // still inside its own two-second grace
		assertTrue(runtime.tickDecay(def, base + 3_000)); // one second past grace, two per second
		assertEquals(2, runtime.stacks("Swift"));
	}

	@Test
	void eachPassiveIsCountedSeparatelyAndOnlyForItsOwnTrigger() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2, 2);
		ClassDefinition def = withPassives(warrior().passives().get(0), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(def, "on_hit_taken", base);
		runtime.onTrigger(def, "on_hit_taken", base);

		assertEquals(2, runtime.stacks("Bulwark"));
		assertEquals(0, runtime.stacks("Swift"));
	}

	private static ClassDefinition warrior() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR);
	}

	/** A class carrying exactly the given passives, so a rule can be read off a passive of our own. */
	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(passives), null, 0, "", List.of(), "");
	}
}
