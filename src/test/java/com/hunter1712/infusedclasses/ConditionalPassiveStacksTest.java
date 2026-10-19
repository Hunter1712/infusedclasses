package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Conditional-passive stacks: the transient half of per-player state. Time is passed in rather
 * than read, so the feed, the per-second bleed and the price are asserted without waiting, and
 * the class is passed in rather than looked up, so this module holds no reference to a player's
 * choice. Stacks are addressed by the passive definition, the same way production does.
 */
class ConditionalPassiveStacksTest {
	/** A trigger of our own, so per-passive counting can be read off a passive the registry never carries. */
	private static final Trigger ON_LAND = new Trigger("on_land", "Land a swing");
	/** A trigger nothing in the mod emits: feeding it must earn nothing. */
	private static final Trigger NEVER_EMITTED = new Trigger("on_death", "Die");

	@Test
	void hitDealtAndHitTakenEachFeedOne() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;

		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_TAKEN, base, 1.0F);

		assertEquals(2, runtime.stacks(momentum()));
	}

	@Test
	void stacksCapAtMaxStacks() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;

		for (int i = 0; i < 70; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);
		}

		assertEquals(Momentum.MAX_STACKS, runtime.stacks(momentum()));
	}

	@Test
	void spamFeedsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();

		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 0.5F);

		assertEquals(0, runtime.stacks(momentum()));
	}

	@Test
	void unknownTriggerAndMissingClassAreInert() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;

		runtime.onTrigger(juggernaut(), NEVER_EMITTED, base, 1.0F);

		assertEquals(0, runtime.stacks(momentum()));

		PlayerRuntime noClass = new PlayerRuntime();
		noClass.onTrigger(null, Trigger.ON_HIT_DEALT, base, 1.0F);

		assertEquals(0, noClass.stacks(momentum()));
	}

	@Test
	void stacksAreTransientNotPersistedOrCopied() {
		PlayerClasses oldClasses = new PlayerClasses();
		oldClasses.trySetClass(ClassRegistry.JUGGERNAUT);
		PlayerRuntime oldRuntime = new PlayerRuntime();
		oldRuntime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, System.currentTimeMillis(), 1.0F);

		assertEquals(ClassRegistry.JUGGERNAUT.toString(), oldClasses.writeId());

		// Stacks live in the transient half and never reach the saved payload: the whole
		// persisted content is still the one choice id, however many stacks have been earned.
		oldRuntime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, System.currentTimeMillis(), 1.0F);
		assertEquals(2, oldRuntime.stacks(momentum()));
		assertEquals(ClassRegistry.JUGGERNAUT.toString(), oldClasses.writeId());

		// The respawn copy carries the choice and nothing else, and reads back as the same id.
		PlayerClasses newClasses = new PlayerClasses();
		newClasses.copyFrom(oldClasses);

		assertEquals(ClassRegistry.JUGGERNAUT.toString(), newClasses.writeId());
		assertEquals(ClassRegistry.JUGGERNAUT, newClasses.currentDefinition().id());
	}

	@Test
	void quietFiveSecondsLoseNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);

		runtime.decay(juggernaut(), base + 5_000);

		assertEquals(1, runtime.stacks(momentum()));
	}

	@Test
	void aMillisecondPastTheGraceLosesOneThenOnePerSecond() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);
		}

		runtime.decay(juggernaut(), base + 5_001);
		assertEquals(4, runtime.stacks(momentum()));

		runtime.decay(juggernaut(), base + 6_000);
		assertEquals(4, runtime.stacks(momentum()), "the next loss is not owed until its own second");

		runtime.decay(juggernaut(), base + 6_001);
		assertEquals(3, runtime.stacks(momentum()));
	}

	@Test
	void aLongQuietBarCatchesUpInOneStep() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);
		}

		runtime.decay(juggernaut(), base + 10_000);

		assertEquals(0, runtime.stacks(momentum()));
	}

	@Test
	void aFeedAtAnyPointRestartsTheBleed() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);

		runtime.decay(juggernaut(), base + 4_000);
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base + 4_000, 1.0F);
		runtime.decay(juggernaut(), base + 8_000);

		assertEquals(2, runtime.stacks(momentum()));

		runtime.decay(juggernaut(), base + 9_001);
		assertEquals(1, runtime.stacks(momentum()));
	}

	@Test
	void theFloorIsZeroAndNeverNegative() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);

		runtime.decay(juggernaut(), 1_000_000);

		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(0, runtime.graceRemaining(momentum(), 1_000_000));
		assertEquals(0, runtime.graceStamp(momentum()));
	}

	@Test
	void spendLeavesTheRemainderAndReportsWhatItTook() {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < 7; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		}

		assertEquals(5, runtime.spendStacks(momentum(), 5));
		assertEquals(2, runtime.stacks(momentum()));

		assertEquals(2, runtime.spendStacks(momentum(), 5));
		assertEquals(0, runtime.stacks(momentum()));

		assertEquals(0, runtime.spendStacks(momentum(), 5));
	}

	@Test
	void aSpendDoesNotRestartTheBleed() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		for (int i = 0; i < 8; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, base, 1.0F);
		}

		assertEquals(5, runtime.spendStacks(momentum(), 5));
		assertEquals(3, runtime.stacks(momentum()));

		// The feed stamp still runs from the last hit, not from the spend.
		runtime.decay(juggernaut(), base + 5_001);
		assertEquals(2, runtime.stacks(momentum()));
	}

	@Test
	void theDecayStepIsPureAndCoversWholeSeconds() {
		assertEquals(new PassiveStacks.Decay(3, 5_000, 0), PassiveStacks.nextDecay(3, 5_000, 4_000));
		assertEquals(new PassiveStacks.Decay(3, 5_000, 0), PassiveStacks.nextDecay(3, 5_000, 5_000));
		assertEquals(new PassiveStacks.Decay(2, 6_000, 1), PassiveStacks.nextDecay(3, 5_000, 5_001));
		assertEquals(new PassiveStacks.Decay(2, 6_000, 1), PassiveStacks.nextDecay(3, 5_000, 6_000));
		assertEquals(new PassiveStacks.Decay(1, 7_000, 2), PassiveStacks.nextDecay(3, 5_000, 6_001));
		assertEquals(new PassiveStacks.Decay(0, 0, 3), PassiveStacks.nextDecay(3, 5_000, 7_001));
		assertEquals(new PassiveStacks.Decay(0, 0, 0), PassiveStacks.nextDecay(0, 5_000, 9_000));
	}

	@Test
	void capAndGraceComeFromThePassiveNotFromThisModule() {
		ConditionalPassive swift = new ConditionalPassive("Swift",
				List.of(new ConditionalPassive.Earn(ON_LAND, 1)), 4,
				List.of(new ConditionalPassive.Tier(2, "Quick", 0.5, -0.001, 0.25)), 2);
		ClassDefinition def = withPassives(swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;

		for (int i = 0; i < 6; i++) {
			runtime.onTrigger(def, ON_LAND, base, 1.0F);
		}
		assertEquals(4, runtime.stacks(swift));

		runtime.decay(def, base + 2_000);
		assertEquals(4, runtime.stacks(swift), "still inside its own two-second grace");

		runtime.decay(def, base + 2_001);
		assertEquals(3, runtime.stacks(swift), "past grace, one per second");
	}

	@Test
	void eachPassiveIsCountedSeparatelyAndOnlyForItsOwnTrigger() {
		ConditionalPassive swift = new ConditionalPassive("Swift",
				List.of(new ConditionalPassive.Earn(ON_LAND, 1)), 4,
				List.of(new ConditionalPassive.Tier(2, "Quick", 0.5, -0.001, 0.25)), 2);
		ClassDefinition def = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;

		runtime.onTrigger(def, Trigger.ON_HIT_DEALT, base, 1.0F);
		runtime.onTrigger(def, Trigger.ON_HIT_DEALT, base, 1.0F);

		assertEquals(2, runtime.stacks(momentum()));
		assertEquals(0, runtime.stacks(swift));

		runtime.onTrigger(def, ON_LAND, base, 1.0F);

		assertEquals(1, runtime.stacks(swift));
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
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(passives), List.of(), 0, "", List.of(), List.of(), CritRule.universal());
	}
}
