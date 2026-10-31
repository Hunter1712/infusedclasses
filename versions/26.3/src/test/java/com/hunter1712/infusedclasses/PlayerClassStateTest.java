package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerClassStateTest {
	@Test
	void acceptsJuggernautThroughPurePath() {
		PlayerClasses classes = new PlayerClasses();

		ClassDefinition def = classes.trySetClass(ClassRegistry.JUGGERNAUT);

		assertEquals(ClassRegistry.JUGGERNAUT, def.id());
		assertEquals(ClassRegistry.JUGGERNAUT, classes.classId());
		assertEquals(ClassRegistry.JUGGERNAUT, classes.currentDefinition().id());
	}

	@Test
	void rejectsUnknownAndMissing() {
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
		classes.trySetClass(ClassRegistry.JUGGERNAUT);

		assertNull(classes.trySetClass(Identifier.fromNamespaceAndPath("infusedclasses", "mage")));

		assertEquals(ClassRegistry.JUGGERNAUT, classes.classId());
	}

	@Test
	void respawnCopyPreservesClassOnFreshInstance() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.JUGGERNAUT);
		PlayerClasses newClasses = new PlayerClasses();

		newClasses.copyFrom(oldClasses);

		assertEquals(ClassRegistry.JUGGERNAUT, newClasses.classId());
	}
}
