package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stack's rows decision, asserted without a screen: roster per class, riders at
 * their window boundaries, whole-second rounding, ready versus lit.
 */
class StatusHudTest {
	private static final String JUGGERNAUT = "infusedclasses:juggernaut";
	private static final String LEGIONARY = "infusedclasses:legionary";
	private static final String GAMBLER = "infusedclasses:gambler";
	/** The Momentum word comes from the passive the row reads, not a HUD copy. */
	private static final String MOMENTUM =
			ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passiveForTrigger(Trigger.ON_HIT_DEALT).name();

	/**
	 * A mutable snapshot template: tests set the few reads under test, then build. The
	 * pending flags mirror the runtime's own read — a lane holds its one-shot while its
	 * window lives, Rotate's only when the landing was Gladius — so a snapshot this
	 * builds reads like one the server would send.
	 */
	private static final class Snap {
		String classId = JUGGERNAUT;
		final Map<Lane, Long> lanes = new EnumMap<>(Lane.class);
		final Map<Lane, Long> windows = new EnumMap<>(Lane.class);
		/** Lanes whose one-shot was spent: the only pending flag a test sets by hand. */
		final Set<Lane> spent = EnumSet.noneOf(Lane.class);
		RotateAbility.Stance flipTarget;
		RouletteAbility.Outcome outcome;
		int stacks;
		long grace;
		long graceStamp;
		RotateAbility.Stance stance = RotateAbility.Stance.SCUTUM;
		int streak;

		Snap lane(Lane lane, long remainingMs) {
			lanes.put(lane, remainingMs);
			return this;
		}

		Snap window(Lane lane, long remainingMs) {
			windows.put(lane, remainingMs);
			return this;
		}

		Snap spend(Lane lane) {
			spent.add(lane);
			return this;
		}

		PlayerStatusPayload build() {
			EnumMap<Lane, LaneRead> reads = new EnumMap<>(Lane.class);
			for (Lane lane : Lane.values()) {
				long windowMs = windows.getOrDefault(lane, 0L);
				boolean pending = windowMs > 0 && !spent.contains(lane)
						&& (lane != Lane.ROTATE || flipTarget == RotateAbility.Stance.GLADIUS);
				reads.put(lane, new LaneRead(lanes.getOrDefault(lane, 0L), windowMs, pending));
			}
			return new PlayerStatusPayload(classId,
					new PlayerRuntime.State(reads, flipTarget, outcome, stance, streak),
					new PlayerRuntime.BarRead(stacks, grace, graceStamp));
		}
	}

	private static List<StatusHud.Row> rows(PlayerStatusPayload status, long elapsedMs) {
		return StatusHud.rows(status, elapsedMs);
	}

	private static StatusHud.Row row(PlayerStatusPayload status, int index) {
		return rows(status, 0).get(index);
	}

	private static StatusHud.Chip chip(PlayerStatusPayload status, int rowIndex, int chipIndex) {
		return row(status, rowIndex).chips().get(chipIndex);
	}

	private static StatusHud.Chip lane(String label, String value) {
		return new StatusHud.Chip(label, value, true, false);
	}

	private static StatusHud.Chip ready(String label) {
		return new StatusHud.Chip(label, "", false, false);
	}

	private static StatusHud.Chip rider(String label, String value) {
		return new StatusHud.Chip(label, value, true, true);
	}

	@Test
	void classlessAndUnknownRenderNoRows() {
		Snap classless = new Snap();
		classless.classId = "";
		assertEquals(List.of(), rows(classless.build(), 0));

		Snap unknown = new Snap();
		unknown.classId = "infusedclasses:nope";
		assertEquals(List.of(), rows(unknown.build(), 0));
	}

	@Test
	void readyJuggernautShowsTwoDimWordLanes() {
		assertEquals(List.of(
				new StatusHud.Row(List.of(ready(AbilityRoster.OFFLOAD.name()))),
				new StatusHud.Row(List.of(ready(AbilityRoster.ONSLAUGHT.name())))),
				rows(new Snap().build(), 0));
	}

	@Test
	void coolingLanesLightUpWithWholeSecondNumbers() {
		long[] remaining = {60_000, 59_999, 10_000, 9_999, 1_000, 999, 1};
		String[] text = {"60s", "60s", "10s", "10s", "1s", "1s", "1s"};
		for (int i = 0; i < remaining.length; i++) {
			Snap snap = new Snap().lane(Lane.ONSLAUGHT, remaining[i]);
			assertEquals(text[i], chip(snap.build(), 1, 0).value(), "onslaught " + remaining[i] + "ms");
			assertTrue(chip(snap.build(), 1, 0).lit());
			assertEquals(AbilityRoster.ONSLAUGHT.name(), chip(snap.build(), 1, 0).label());
		}
	}

	@Test
	void elapsedCountsDownAndReadyLandsAtItsBoundary() {
		PlayerStatusPayload status = new Snap().lane(Lane.OFFLOAD, 10_000).build();

		assertEquals("5s", rows(status, 5_000).get(0).chips().get(0).value());
		assertEquals("1s", rows(status, 9_001).get(0).chips().get(0).value());
		assertEquals(ready(AbilityRoster.OFFLOAD.name()), rows(status, 10_000).get(0).chips().get(0));
		assertEquals(ready(AbilityRoster.OFFLOAD.name()), rows(status, 15_000).get(0).chips().get(0));
	}

	@Test
	void offloadWindowRiderIsABareNumberForItsWholeLife() {
		Snap snap = new Snap().window(Lane.OFFLOAD, 5_000);
		assertEquals(rider("", "5s"), chip(snap.build(), 0, 1));

		// The price is paid at the press: the countdown is the whole story.
		snap.spend(Lane.OFFLOAD);
		assertEquals(rider("", "5s"), chip(snap.build(), 0, 1));

		// The rider counts with elapsed; the window expiring drops it.
		assertEquals(rider("", "3s"), rows(snap.build(), 2_000).get(0).chips().get(1));
		assertEquals(1, rows(snap.build(), 5_000).get(0).chips().size());
	}

	@Test
	void onslaughtWindowRiderCountsDownUntilTheWindowEnds() {
		Snap snap = new Snap().window(Lane.ONSLAUGHT, 15_000);
		assertEquals(rider("", "15s"), chip(snap.build(), 1, 1));
		assertEquals(rider("", "1s"), rows(snap.build(), 14_000).get(1).chips().get(1));
		assertEquals(1, rows(snap.build(), 15_000).get(1).chips().size());
	}

	@Test
	void rotateRidersSplitByFlipTarget() {
		Snap impetus = new Snap();
		impetus.classId = LEGIONARY;
		impetus.window(Lane.ROTATE, 5_000);
		impetus.flipTarget = RotateAbility.Stance.GLADIUS;
		assertEquals(rider("", "5s"), chip(impetus.build(), 1, 1));

		// Spent Impetus is gone while the flip window still runs toward gladius.
		impetus.spend(Lane.ROTATE);
		assertEquals(1, row(impetus.build(), 1).chips().size());

		Snap testudo = new Snap();
		testudo.classId = LEGIONARY;
		testudo.window(Lane.ROTATE, 1_500);
		testudo.flipTarget = RotateAbility.Stance.SCUTUM;
		assertEquals(rider("", "2s"), chip(testudo.build(), 1, 1));

		// No flip window, no rider.
		Snap idle = new Snap();
		idle.classId = LEGIONARY;
		assertEquals(1, row(idle.build(), 1).chips().size());
	}

	@Test
	void rouletteOutcomeRiderCarriesTheBandWord() {
		Snap snap = new Snap();
		snap.classId = GAMBLER;
		snap.window(Lane.ROULETTE, 10_000);

		snap.outcome = RouletteAbility.Outcome.EDGE;
		assertEquals(rider("Edge", "10s"), chip(snap.build(), 0, 1));
		snap.outcome = RouletteAbility.Outcome.WARD;
		assertEquals(rider("Ward", "10s"), chip(snap.build(), 0, 1));
		snap.outcome = RouletteAbility.Outcome.HASTE;
		assertEquals(rider("Haste", "10s"), chip(snap.build(), 0, 1));
		snap.outcome = RouletteAbility.Outcome.JACKPOT;
		assertEquals(rider("Jackpot", "10s"), chip(snap.build(), 0, 1));
		snap.outcome = RouletteAbility.Outcome.BUST;
		assertEquals(rider("Bust", "10s"), chip(snap.build(), 0, 1));

		snap.outcome = RouletteAbility.Outcome.ZERO;
		assertEquals(1, row(snap.build(), 0).chips().size());
		snap.outcome = null;
		assertEquals(1, row(snap.build(), 0).chips().size());
	}

	@Test
	void momentumRowCarriesStacksAndGraceAndVanishesAtZero() {
		Snap snap = new Snap();
		snap.stacks = 12;
		snap.grace = 4_200;

		assertEquals(new StatusHud.Chip(MOMENTUM, "12 \u00b7 5s", true, false), chip(snap.build(), 2, 0));
		assertEquals(new StatusHud.Chip(MOMENTUM, "12 \u00b7 4s", true, false),
				rows(snap.build(), 1_000).get(2).chips().get(0));

		snap.stacks = 0;
		snap.grace = 0;
		assertEquals(2, rows(snap.build(), 0).size());
	}

	@Test
	void momentumRowCarriesItsCheckpointWord() {
		Snap snap = new Snap();

		snap.stacks = 15;
		assertEquals(rider("Steady", ""), chip(snap.build(), 2, 1));

		snap.stacks = 30;
		assertEquals(rider("Braced", ""), chip(snap.build(), 2, 1));

		snap.stacks = 45;
		assertEquals(rider("Rooted", ""), chip(snap.build(), 2, 1));

		// The reserve above the top checkpoint stays Rooted.
		snap.stacks = Momentum.MAX_STACKS;
		assertEquals(rider("Rooted", ""), chip(snap.build(), 2, 1));
	}

	@Test
	void aBarBelowTheFirstCheckpointDrawsNoRider() {
		Snap snap = new Snap();
		snap.stacks = 14;

		assertEquals(1, row(snap.build(), 2).chips().size());
	}

	@Test
	void momentumGraceNeverPrintsZero() {
		Snap snap = new Snap();
		snap.stacks = 12;
		snap.grace = 4_200;
		PlayerStatusPayload status = snap.build();

		// The grace spent out while the snapshot still carries stacks: bare stacks, no `0s`.
		assertEquals(new StatusHud.Chip(MOMENTUM, "12", true, false), rows(status, 4_200).get(2).chips().get(0));
		assertEquals(new StatusHud.Chip(MOMENTUM, "12", true, false), rows(status, 9_000).get(2).chips().get(0));
	}

	@Test
	void streakRowOnlyAboveZero() {
		Snap snap = new Snap();
		snap.classId = GAMBLER;
		snap.streak = 3;

		assertEquals(new StatusHud.Chip(CritRule.STREAK, "3", true, false), chip(snap.build(), 2, 0));

		snap.streak = 0;
		assertEquals(2, rows(snap.build(), 0).size());
	}

	@Test
	void legionaryStanceRowLeadsWithItsStanceWord() {
		Snap gladius = new Snap();
		gladius.classId = LEGIONARY;
		gladius.stance = RotateAbility.Stance.GLADIUS;
		assertEquals(new StatusHud.Chip(RotateAbility.Stance.GLADIUS.displayName(), "", true, false), chip(gladius.build(), 0, 0));

		Snap scutum = new Snap();
		scutum.classId = LEGIONARY;
		assertEquals(new StatusHud.Chip(RotateAbility.Stance.SCUTUM.displayName(), "", true, false), chip(scutum.build(), 0, 0));
		// Stance row, signature row, ultimate row: no state rows for a Legionary.
		assertEquals(3, rows(scutum.build(), 0).size());
	}

	@Test
	void ultimateRidersAreBareNumbers() {
		Snap oath = new Snap();
		oath.classId = LEGIONARY;
		oath.window(Lane.OATH, 6_000);
		assertEquals(rider("", "6s"), chip(oath.build(), 2, 1));

		Snap allIn = new Snap();
		allIn.classId = GAMBLER;
		allIn.window(Lane.ALL_IN, 15_000);
		assertEquals(rider("", "15s"), chip(allIn.build(), 1, 1));
	}

	@Test
	void slotRowsDrawTheRowsTheSlotsFire() {
		// The Gambler's signature line is the wheel's row and its ultimate line All In's —
		// the same shape the binds fire from, so the stack cannot draw another ability.
		Snap gambler = new Snap();
		gambler.classId = GAMBLER;
		gambler.lane(Lane.ROULETTE, 25_000);
		gambler.lane(Lane.ALL_IN, 75_000);

		assertEquals(List.of(
				new StatusHud.Row(List.of(lane(RouletteAbility.NAME, "25s"))),
				new StatusHud.Row(List.of(lane(AllInAbility.NAME, "75s")))),
				rows(gambler.build(), 0));
	}
}
