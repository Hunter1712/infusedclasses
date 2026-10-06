package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Transient lifetime behind one interface: no player needed. */
class PlayerRuntimesTest {
	@Test
	void sameUuidSeesOneRuntime() {
		PlayerRuntimes store = new PlayerRuntimes();
		UUID id = UUID.randomUUID();

		assertSame(store.runtime(id), store.runtime(id));
	}

	@Test
	void removeDropsTheEntry() {
		PlayerRuntimes store = new PlayerRuntimes();
		UUID id = UUID.randomUUID();
		PlayerRuntime first = store.runtime(id);
		first.onTrigger(ClassRegistry.byId(ClassRegistry.WARRIOR), "on_hit_taken", 0);

		store.remove(id);

		assertNotSame(first, store.runtime(id));
		assertEquals(0, store.runtime(id).stacks(
				ClassRegistry.byId(ClassRegistry.WARRIOR).passives().get(0)));
	}

	@Test
	void respawnRemovesBeforeCreating() {
		PlayerRuntimes store = new PlayerRuntimes();
		UUID id = UUID.randomUUID();
		PlayerRuntime old = store.runtime(id);
		old.onTrigger(ClassRegistry.byId(ClassRegistry.WARRIOR), "on_hit_taken", 0);

		store.respawn(id);

		PlayerRuntime fresh = store.runtime(id);
		assertNotSame(old, fresh);
		assertEquals(0, fresh.stacks(
				ClassRegistry.byId(ClassRegistry.WARRIOR).passives().get(0)));
	}
}
