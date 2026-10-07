package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerClassStateTest {
	@Test
	void acceptsBerserkerThroughPurePath() {
		PlayerClasses classes = new PlayerClasses();

		ClassDefinition def = classes.trySetClass(ClassRegistry.BERSERKER);

		assertEquals(ClassRegistry.BERSERKER, def.id());
		assertEquals(ClassRegistry.BERSERKER, classes.classId());
		assertEquals(ClassRegistry.BERSERKER, classes.currentDefinition().id());
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
		classes.trySetClass(ClassRegistry.BERSERKER);

		assertNull(classes.trySetClass(Identifier.fromNamespaceAndPath("infusedclasses", "mage")));

		assertEquals(ClassRegistry.BERSERKER, classes.classId());
	}

	@Test
	void respawnCopyPreservesClassOnFreshInstance() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.BERSERKER);
		PlayerClasses newClasses = new PlayerClasses();

		newClasses.copyFrom(oldClasses);

		assertEquals(ClassRegistry.BERSERKER, newClasses.classId());
	}
}
