package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
