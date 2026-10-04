package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionalPassiveStacksTest {
	@Test
	void hitTakenIncrementsStacksWhileWarriorSelected() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);

		classes.onTrigger("on_hit_taken");
		classes.onTrigger("on_hit_taken");

		assertEquals(2, classes.stacks("Bulwark"));
	}

	@Test
	void stacksCapAtMaxStacks() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);

		for (int i = 0; i < 15; i++) {
			classes.onTrigger("on_hit_taken");
		}

		assertEquals(10, classes.stacks("Bulwark"));
	}

	@Test
	void unknownTriggerAndMissingClassAreInert() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);

		classes.onTrigger("on_death");

		assertEquals(0, classes.stacks("Bulwark"));

		PlayerClasses noClass = new PlayerClasses();
		noClass.onTrigger("on_hit_taken");

		assertEquals(0, noClass.stacks("Bulwark"));
	}

	@Test
	void stacksAreTransientNotPersistedOrCopied() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.WARRIOR);
		oldClasses.onTrigger("on_hit_taken");

		assertEquals(ClassRegistry.WARRIOR.toString(), oldClasses.writeId());

		PlayerClasses newClasses = new PlayerClasses();
		newClasses.copyFrom(oldClasses);

		assertEquals(0, newClasses.stacks("Bulwark"));
	}

	@Test
	void noDecayWithinGracePeriod() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
		long base = System.currentTimeMillis();
		classes.onTrigger("on_hit_taken", base);

		// Tick at 4 seconds after trigger - still in grace period
		boolean changed = classes.tickDecay(base + 4_000);

		assertFalse(changed);
		assertEquals(1, classes.stacks("Bulwark"));
	}

	@Test
	void decayStartsAfterGracePeriod() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
		long base = System.currentTimeMillis();
		classes.onTrigger("on_hit_taken", base);

		// Tick at 6 seconds after trigger - 1 second past grace, should lose 1
		boolean changed = classes.tickDecay(base + 6_000);

		assertTrue(changed);
		assertEquals(0, classes.stacks("Bulwark"));
	}

	@Test
	void decayMultipleStacksAfterExtendedTime() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
		long base = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			classes.onTrigger("on_hit_taken", base);
		}
		assertEquals(5, classes.stacks("Bulwark"));

		// Tick at 10 seconds after trigger - 5 seconds past grace, should lose 5
		boolean changed = classes.tickDecay(base + 10_000);

		assertTrue(changed);
		assertEquals(0, classes.stacks("Bulwark"));
	}

	@Test
	void newTriggerResetsGraceTimer() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
		long base = System.currentTimeMillis();
		classes.onTrigger("on_hit_taken", base);

		// Advance 4 seconds, then trigger again
		classes.tickDecay(base + 4_000);
		classes.onTrigger("on_hit_taken", base + 4_000); // should reset grace

		// Now advance 4 more seconds (8 total from first hit, 4 from second)
		// Since second hit reset grace, still in grace period
		classes.tickDecay(base + 8_000);

		assertEquals(2, classes.stacks("Bulwark"));
	}
}
