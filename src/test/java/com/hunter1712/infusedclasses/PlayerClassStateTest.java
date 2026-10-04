package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerClassStateTest {
	@Test
	void acceptsWarriorThroughPurePath() {
		PlayerClasses classes = new PlayerClasses();

		ClassDefinition def = classes.trySetClass(ClassRegistry.WARRIOR);

		assertEquals(ClassRegistry.WARRIOR, def.id());
		assertEquals(ClassRegistry.WARRIOR, classes.classId());
		assertEquals(ClassRegistry.WARRIOR, classes.currentDefinition().id());
	}

	@Test
	void rejectsLockedUnknownAndMissing() {
		PlayerClasses classes = new PlayerClasses();

		assertNull(classes.trySetClass(Identifier.fromNamespaceAndPath("infusedclasses", "ranger")));
		assertNull(classes.trySetClass(Identifier.fromNamespaceAndPath("infusedclasses", "bard")));
		assertNull(classes.trySetClass(null));
		assertNull(classes.classId());
		assertNull(classes.currentDefinition());
	}

	@Test
	void rejectedPickKeepsPreviousClass() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);

		assertNull(classes.trySetClass(Identifier.fromNamespaceAndPath("infusedclasses", "mage")));

		assertEquals(ClassRegistry.WARRIOR, classes.classId());
	}

	@Test
	void respawnCopyPreservesClassOnFreshInstance() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.WARRIOR);
		PlayerClasses newClasses = new PlayerClasses();

		newClasses.copyFrom(oldClasses);

		assertEquals(ClassRegistry.WARRIOR, newClasses.classId());
	}
}
