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
 * in rather than looked up, so this module holds no reference to a player's choice. Stacks are
 * addressed by the passive definition, the same way production does.
 */
class ConditionalPassiveStacksTest {
	@Test
	void hitTakenIncrementsStacksWhileWarriorSelected() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(warrior(), "on_hit_taken", base);
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		assertEquals(2, runtime.stacks(bulwark()));
	}

	@Test
	void stacksCapAtMaxStacks() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		for (int i = 0; i < 15; i++) {
			runtime.onTrigger(warrior(), "on_hit_taken", base);
		}

		assertEquals(10, runtime.stacks(bulwark()));
	}

	@Test
	void unknownTriggerAndMissingClassAreInert() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(warrior(), "on_death", base);

		assertEquals(0, runtime.stacks(bulwark()));

		PlayerRuntime noClass = new PlayerRuntime();
		noClass.onTrigger(null, "on_hit_taken", base);

		assertEquals(0, noClass.stacks(bulwark()));
	}

	@Test
	void stacksAreTransientNotPersistedOrCopied() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.WARRIOR);
		PlayerRuntime oldRuntime = new PlayerRuntime();
		oldRuntime.onTrigger(warrior(), "on_hit_taken", System.currentTimeMillis());

		assertEquals(ClassRegistry.WARRIOR.toString(), oldClasses.writeId());

		// Stacks live in the transient half and never reach the saved payload: the whole
		// persisted content is still the one choice id, however many stacks have been earned.
		oldRuntime.onTrigger(warrior(), "on_hit_taken", System.currentTimeMillis());
		assertEquals(2, oldRuntime.stacks(bulwark()));
		assertEquals(ClassRegistry.WARRIOR.toString(), oldClasses.writeId());

		// The respawn copy carries the choice and nothing else, and reads back as the same id.
		// The new player's own stacks are zero because the server entry drops the old runtime
		// and creates a fresh one under the shared UUID, which is wiring the suite cannot reach
		// without a live server player; that ordering is marked in the entry instead.
		PlayerClasses newClasses = new PlayerClasses();
		newClasses.copyFrom(oldClasses);

		assertEquals(ClassRegistry.WARRIOR.toString(), newClasses.writeId());
		assertEquals(ClassRegistry.WARRIOR, newClasses.currentDefinition().id());
	}

	@Test
	void noDecayWithinGracePeriod() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		// Tick at 4 seconds after trigger - still in grace period
		boolean changed = !runtime.tickDecayMoved(warrior(), base + 4_000).isEmpty();

		assertFalse(changed);
		assertEquals(1, runtime.stacks(bulwark()));
	}

	@Test
	void decayStartsAfterGracePeriod() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		// Tick at 6 seconds after trigger - 1 second past grace, should lose 1
		boolean changed = !runtime.tickDecayMoved(warrior(), base + 6_000).isEmpty();

		assertTrue(changed);
		assertEquals(0, runtime.stacks(bulwark()));
	}

	@Test
	void decayMultipleStacksAfterExtendedTime() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(warrior(), "on_hit_taken", base);
		}
		assertEquals(5, runtime.stacks(bulwark()));

		// Tick at 10 seconds after trigger - 5 seconds past grace, should lose 5
		boolean changed = !runtime.tickDecayMoved(warrior(), base + 10_000).isEmpty();

		assertTrue(changed);
		assertEquals(0, runtime.stacks(bulwark()));
	}

	@Test
	void dropFiresPastGraceMillisNotTruncatedSeconds() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, base);

		// Exactly 5s: still grace, the client agrees. Past it by a millisecond: gone on both.
		assertTrue(runtime.tickDecayMoved(warrior(), base + 5_000).isEmpty());
		assertEquals(1, runtime.stacks(bulwark()));
		assertFalse(runtime.tickDecayMoved(warrior(), base + 5_001).isEmpty());
		assertEquals(0, runtime.stacks(bulwark()));
	}

	@Test
	void pastGraceDropsEveryStackAtOnce() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(warrior(), "on_hit_taken", base);
		}
		assertEquals(5, runtime.stacks(bulwark()));

		// Tick at +6s drops all 5 at once; the next tick has nothing left to move.
		assertFalse(runtime.tickDecayMoved(warrior(), base + 6_000).isEmpty());
		assertEquals(0, runtime.stacks(bulwark()));
		assertTrue(runtime.tickDecayMoved(warrior(), base + 7_000).isEmpty());
	}

	@Test
	void newTriggerResetsGraceTimer() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();
		runtime.onTrigger(warrior(), "on_hit_taken", base);

		// Advance 4 seconds, then trigger again
		runtime.tickDecayMoved(warrior(), base + 4_000);
		runtime.onTrigger(warrior(), "on_hit_taken", base + 4_000); // should reset grace

		// Now advance 4 more seconds (8 total from first hit, 4 from second)
		// Since second hit reset grace, still in grace period
		runtime.tickDecayMoved(warrior(), base + 8_000);

		assertEquals(2, runtime.stacks(bulwark()));
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
		assertEquals(4, runtime.stacks(swift)); // its own cap, not the ten Bulwark allows

		assertTrue(runtime.tickDecayMoved(def, base + 2_000).isEmpty()); // still inside its own two-second grace
		assertFalse(runtime.tickDecayMoved(def, base + 3_000).isEmpty()); // past grace, every stack drops at once
		assertEquals(0, runtime.stacks(swift));
	}

	@Test
	void eachPassiveIsCountedSeparatelyAndOnlyForItsOwnTrigger() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = withPassives(bulwark(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = System.currentTimeMillis();

		runtime.onTrigger(def, "on_hit_taken", base);
		runtime.onTrigger(def, "on_hit_taken", base);

		assertEquals(2, runtime.stacks(bulwark()));
		assertEquals(0, runtime.stacks(swift));
	}

	private static ClassDefinition warrior() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR);
	}

	/** The one passive on the warrior, addressed the way the entry addresses it. */
	private static ConditionalPassive bulwark() {
		return warrior().passives().get(0);
	}

	/** A class carrying exactly the given passives, so a rule can be read off a passive of our own. */
	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(passives), null, 0, "", "", "");
	}
}
