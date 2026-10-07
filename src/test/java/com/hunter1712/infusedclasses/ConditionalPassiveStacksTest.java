package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conditional-passive stacks: the transient half of per-player state. Time is passed in rather
 * than read, so decay and the grace period are asserted without waiting, and the class is passed
 * in rather than looked up, so this module holds no reference to a player's choice. Stacks are
 * addressed by the passive definition, the same way production does.
 */
class ConditionalPassiveStacksTest {
	@Test
	void hitDealtIncrementsStacksWhileJuggernautSelected() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(juggernaut(), "on_hit_dealt", base);
		runtime.onTrigger(juggernaut(), "on_hit_dealt", base);

		assertEquals(2, runtime.stacks(momentum()));
	}

	@Test
	void stacksCapAtMaxStacks() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		for (int i = 0; i < 25; i++) {
			runtime.onTrigger(juggernaut(), "on_hit_dealt", base);
		}

		assertEquals(10, runtime.stacks(momentum()));
	}

	@Test
	void unknownTriggerAndMissingClassAreInert() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(juggernaut(), "on_death", base);

		assertEquals(0, runtime.stacks(momentum()));

		PlayerRuntime noClass = new PlayerRuntime();
		noClass.onTrigger(null, "on_hit_dealt", base);

		assertEquals(0, noClass.stacks(momentum()));
	}

	@Test
	void stacksAreTransientNotPersistedOrCopied() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.JUGGERNAUT);
		PlayerRuntime oldRuntime = new PlayerRuntime();
		oldRuntime.onTrigger(juggernaut(), "on_hit_dealt", System.currentTimeMillis());

		assertEquals(ClassRegistry.JUGGERNAUT.toString(), oldClasses.writeId());

		// Stacks live in the transient half and never reach the saved payload: the whole
		// persisted content is still the one choice id, however many stacks have been earned.
		oldRuntime.onTrigger(juggernaut(), "on_hit_dealt", System.currentTimeMillis());
		assertEquals(2, oldRuntime.stacks(momentum()));
		assertEquals(ClassRegistry.JUGGERNAUT.toString(), oldClasses.writeId());

		// The respawn copy carries the choice and nothing else, and reads back as the same id.
		// The new player's own stacks are zero because the server entry drops the old runtime
		// and creates a fresh one under the shared UUID, which is wiring the suite cannot reach
		// without a live server player; that ordering is marked in the entry instead.
		PlayerClasses newClasses = new PlayerClasses();
		newClasses.copyFrom(oldClasses);

		assertEquals(ClassRegistry.JUGGERNAUT.toString(), newClasses.writeId());
		assertEquals(ClassRegistry.JUGGERNAUT, newClasses.currentDefinition().id());
	}

	@Test
	void noDecayWithinGracePeriod() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(juggernaut(), "on_hit_dealt", base);

		// Tick at 3 seconds after trigger - still in grace period
		boolean changed = !runtime.tickDecayMoved(juggernaut(), base + 3_000).isEmpty();

		assertFalse(changed);
		assertEquals(1, runtime.stacks(momentum()));
	}

	@Test
	void decayStartsAfterGracePeriod() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(juggernaut(), "on_hit_dealt", base);

		// Tick at 5 seconds after trigger - 1 second past grace, should lose 1
		boolean changed = !runtime.tickDecayMoved(juggernaut(), base + 5_000).isEmpty();

		assertTrue(changed);
		assertEquals(0, runtime.stacks(momentum()));
	}

	@Test
	void decayMultipleStacksAfterExtendedTime() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), "on_hit_dealt", base);
		}
		assertEquals(5, runtime.stacks(momentum()));

		// Tick at 10 seconds after trigger - 6 seconds past grace, should lose 5
		boolean changed = !runtime.tickDecayMoved(juggernaut(), base + 10_000).isEmpty();

		assertTrue(changed);
		assertEquals(0, runtime.stacks(momentum()));
	}

	@Test
	void dropFiresPastGraceMillisNotTruncatedSeconds() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, base);

		// Exactly 4s: still grace, the client agrees. Past it by a millisecond: gone on both.
		assertTrue(runtime.tickDecayMoved(juggernaut(), base + 4_000).isEmpty());
		assertEquals(1, runtime.stacks(momentum()));
		assertFalse(runtime.tickDecayMoved(juggernaut(), base + 4_001).isEmpty());
		assertEquals(0, runtime.stacks(momentum()));
	}

	@Test
	void pastGraceDropsEveryStackAtOnce() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), "on_hit_dealt", base);
		}
		assertEquals(5, runtime.stacks(momentum()));

		// Tick at +5s drops all 5 at once; the next tick has nothing left to move.
		assertFalse(runtime.tickDecayMoved(juggernaut(), base + 5_000).isEmpty());
		assertEquals(0, runtime.stacks(momentum()));
		assertTrue(runtime.tickDecayMoved(juggernaut(), base + 6_000).isEmpty());
	}

	@Test
	void newTriggerResetsGraceTimer() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(juggernaut(), "on_hit_dealt", base);

		// Advance 3 seconds, then trigger again
		runtime.tickDecayMoved(juggernaut(), base + 3_000);
		runtime.onTrigger(juggernaut(), "on_hit_dealt", base + 3_000); // should reset grace

		// Now advance 3 more seconds (6 total from first hit, 3 from second)
		// Since second hit reset grace, still in grace period
		runtime.tickDecayMoved(juggernaut(), base + 6_000);

		assertEquals(2, runtime.stacks(momentum()));
	}

	@Test
	void capAndGraceComeFromThePassiveNotFromThisModule() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = withPassives(swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		for (int i = 0; i < 6; i++) {
			runtime.onTrigger(def, "on_land", base);
		}
		assertEquals(4, runtime.stacks(swift)); // its own cap, not the ten Momentum allows

		assertTrue(runtime.tickDecayMoved(def, base + 2_000).isEmpty()); // still inside its own two-second grace
		assertFalse(runtime.tickDecayMoved(def, base + 3_000).isEmpty()); // past grace, every stack drops at once
		assertEquals(0, runtime.stacks(swift));
	}

	@Test
	void eachPassiveIsCountedSeparatelyAndOnlyForItsOwnTrigger() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_speed"), 0.5, 2);
		ClassDefinition def = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(def, "on_hit_dealt", base);
		runtime.onTrigger(def, "on_hit_dealt", base);

		assertEquals(2, runtime.stacks(momentum()));
		assertEquals(0, runtime.stacks(swift));
	}

	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	/** The one passive on the juggernaut, addressed the way the entry addresses it. */
	private static ConditionalPassive momentum() {
		return juggernaut().passives().get(0);
	}

	/** A class carrying exactly the given passives, so a rule can be read off a passive of our own. */
	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(passives), List.of(), 0.0, List.of(), 0, "", List.of());
	}
}
