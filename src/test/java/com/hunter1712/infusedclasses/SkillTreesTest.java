package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreesTest {
	private static PlayerClasses warrior() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
		return classes;
	}

	private static PlayerClasses richWarrior() {
		PlayerClasses classes = warrior();
		classes.progress().unspent = 30;
		return classes;
	}

	@Test
	void curveIsFastEarlyAndSlowLate() {
		int first = SkillTrees.xpForNextLevel(1);
		int mid = SkillTrees.xpForNextLevel(10);
		int last = SkillTrees.xpForNextLevel(19);

		assertTrue(first < mid);
		assertTrue(mid < last);
		assertTrue(first <= SkillTrees.xpForKill(true) * 2);
		assertEquals(SkillTrees.xpForNextLevel(SkillTrees.MAX_LEVEL),
				SkillTrees.xpForNextLevel(SkillTrees.MAX_LEVEL + 1));
		assertEquals(SkillTrees.xpForNextLevel(SkillTrees.MAX_LEVEL),
				SkillTrees.xpForNextLevel(SkillTrees.MAX_LEVEL + 10));
	}

	@Test
	void meleeKillsEarnBonusXp() {
		assertTrue(SkillTrees.xpForKill(true) > SkillTrees.xpForKill(false));
		assertEquals(SkillTrees.BASE_KILL_XP + SkillTrees.MELEE_BONUS_XP, SkillTrees.xpForKill(true));
	}

	@Test
	void eachLevelBanksOnePoint() {
		PlayerClasses classes = warrior();
		int before = classes.level();

		while (classes.level() == before) {
			classes.addKillXp(true);
		}

		assertEquals(before + 1, classes.level());
		assertEquals(1, classes.unspentPoints());
	}

	@Test
	void ranksAccrueToMaxThenReject() {
		PlayerClasses classes = richWarrior();

		for (int i = 0; i < 5; i++) {
			assertTrue(classes.tryUnlock(SkillTrees.VIGOR_NODE), "rank " + (i + 1));
		}
		assertFalse(classes.tryUnlock(SkillTrees.VIGOR_NODE));
		assertInstanceOf(SkillTrees.Check.Maxed.class,
				SkillTrees.check(classes.progress().ranks, classes.unspentPoints(), SkillTrees.VIGOR_NODE));
		assertEquals(5, SkillTrees.rankOf(classes.progress().ranks, SkillTrees.VIGOR_NODE));
	}

	@Test
	void tiersRejectBelowSpend() {
		PlayerClasses classes = richWarrior();

		assertInstanceOf(SkillTrees.Check.TierLocked.class,
				SkillTrees.check(classes.progress().ranks, classes.unspentPoints(), SkillTrees.WAR_CRY_NODE));
		assertFalse(classes.tryUnlock(SkillTrees.WAR_CRY_NODE));
		assertInstanceOf(SkillTrees.Check.Unknown.class,
				SkillTrees.check(classes.progress().ranks, classes.unspentPoints(), "juggernaut_bard"));
	}

	@Test
	void chainRejectsAugmentWithoutBase() {
		PlayerClasses classes = richWarrior();
		for (int i = 0; i < 5; i++) {
			assertTrue(classes.tryUnlock(SkillTrees.VIGOR_NODE));
		}

		assertInstanceOf(SkillTrees.Check.MissingParents.class,
				SkillTrees.check(classes.progress().ranks, classes.unspentPoints(), SkillTrees.WAR_CRY_NODE));
		assertFalse(classes.tryUnlock(SkillTrees.WAR_CRY_NODE));

		assertTrue(classes.tryUnlock(SkillTrees.THICK_SKIN_NODE));
		assertTrue(classes.tryUnlock(SkillTrees.WAR_CRY_NODE));
	}

	@Test
	void rushPathCostsThirteenToCapstone() {
		PlayerClasses classes = richWarrior();

		for (int i = 0; i < 5; i++) {
			assertTrue(classes.tryUnlock(SkillTrees.VIGOR_NODE));
		}
		assertTrue(classes.tryUnlock(SkillTrees.THICK_SKIN_NODE));
		assertTrue(classes.tryUnlock(SkillTrees.WAR_CRY_NODE));
		for (int i = 0; i < 4; i++) {
			assertTrue(classes.tryUnlock(SkillTrees.BULWARK_NODE));
		}
		assertEquals(12, SkillTrees.spentPoints(classes.progress().ranks));
		assertTrue(classes.tryUnlock(SkillTrees.RALLYING_NODE));
		assertEquals(13, SkillTrees.spentPoints(classes.progress().ranks));

		int refund = classes.respec();
		assertEquals(13, refund);
		assertTrue(classes.progress().ranks.isEmpty());
	}

	@Test
	void bulwarkAlternatesArmorAndToughness() {
		PlayerClasses classes = richWarrior();
		for (int i = 0; i < 5; i++) {
			assertTrue(classes.tryUnlock(SkillTrees.VIGOR_NODE));
		}
		for (int i = 0; i < 3; i++) {
			assertTrue(classes.tryUnlock(SkillTrees.BULWARK_NODE));
		}

		double armor = 0;
		double tough = 0;
		for (Passive p : classes.treePassives()) {
			if (p.attribute().getPath().equals("armor")) {
				armor += p.value();
			}
			if (p.attribute().getPath().equals("armor_toughness")) {
				tough += p.value();
			}
		}
		assertEquals(2.0, armor);
		assertEquals(0.5, tough);
	}

	@Test
	void rallyingAddsAbsorptionAndShorterCooldown() {
		assertTrue(SkillTrees.warCryEffects(java.util.Set.of(SkillTrees.RALLYING_NODE)).absorption());
		assertTrue(SkillTrees.warCryEffects(java.util.Set.of(SkillTrees.RALLYING_NODE)).resistance());
		assertFalse(SkillTrees.warCryEffects(java.util.Set.of(SkillTrees.WAR_CRY_NODE)).absorption());
		assertFalse(SkillTrees.warCryEffects(java.util.Set.of()).resistance());
		assertEquals(20_000L, SkillTrees.warCryCooldownMs(java.util.Set.of(SkillTrees.RALLYING_NODE)));
		assertEquals(Cooldowns.WAR_CRY_MS, SkillTrees.warCryCooldownMs(java.util.Set.of()));
	}

	@Test
	void nonWarriorCannotUnlockJuggernautNodes() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:ranger");
		classes.progress().unspent = 10;
		assertFalse(classes.tryUnlock(SkillTrees.VIGOR_NODE));

		PlayerClasses classless = new PlayerClasses();
		assertEquals(0, classless.addKillXp(false));
		assertFalse(classless.tryUnlock(SkillTrees.VIGOR_NODE));
	}

	@Test
	void lockedBranchesAreDeclared() {
		assertTrue(SkillTrees.LOCKED_BRANCHES.contains("Berserker"));
	}

	@Test
	void rawXpGrantsLevelsWithoutCap() {
		PlayerClasses classes = warrior();
		int gained = classes.addXp(100000);
		assertTrue(classes.level() > SkillTrees.MAX_LEVEL);
		assertEquals(classes.level() - 1, classes.unspentPoints());
		assertTrue(gained > 0);
		assertTrue(classes.addXp(100000) > 0);
		assertEquals(0, classes.addXp(-5));
	}

	@Test
	void thickSkinCapsDecaysAndClears() {
		SkillTrees.ThickSkin skin = new SkillTrees.ThickSkin();
		for (int i = 0; i < 10; i++) {
			skin.hit(1_000L + i);
		}
		assertEquals(SkillTrees.THICK_SKIN_CAP, skin.count());

		assertEquals(SkillTrees.THICK_SKIN_CAP, skin.tick(1_000L + SkillTrees.THICK_SKIN_DECAY_MS - 1));
		assertEquals(0, skin.tick(1_000L + SkillTrees.THICK_SKIN_DECAY_MS + 10_000L));

		skin.hit(50_000L);
		skin.clear();
		assertEquals(0, skin.count());
	}

	@Test
	void enduringSkinRaisesCapAndDecay() {
		assertEquals(SkillTrees.THICK_SKIN_CAP, SkillTrees.capFor(java.util.Set.of()));
		assertEquals(SkillTrees.ENDURING_SKIN_CAP,
				SkillTrees.capFor(java.util.Set.of(SkillTrees.ENDURING_SKIN_NODE)));

		SkillTrees.ThickSkin skin = new SkillTrees.ThickSkin();
		for (int i = 0; i < 20; i++) {
			skin.hit(1_000L + i, SkillTrees.capFor(java.util.Set.of(SkillTrees.ENDURING_SKIN_NODE)));
		}
		assertEquals(SkillTrees.ENDURING_SKIN_CAP, skin.count());
		assertEquals(0, skin.tick(1_000L + SkillTrees.ENDURING_SKIN_DECAY_MS + 50_000L,
				SkillTrees.decayFor(java.util.Set.of(SkillTrees.ENDURING_SKIN_NODE))));
	}

	@Test
	void progressRoundTripsRanks() {
		PlayerClasses classes = warrior();
		classes.progress().unspent = 10;
		assertTrue(classes.tryUnlock(SkillTrees.VIGOR_NODE));
		assertTrue(classes.tryUnlock(SkillTrees.VIGOR_NODE));

		Map<String, SkillTrees.Progress> back = SkillTrees.decodeMap(SkillTrees.encodeMap(Map.of(
				ClassRegistry.WARRIOR.toString(), classes.progress())));
		assertEquals(2, back.get(ClassRegistry.WARRIOR.toString()).ranks.get(SkillTrees.VIGOR_NODE));

		PlayerClasses fresh = new PlayerClasses();
		fresh.readId("infusedclasses:warrior");
		fresh.readTree(classes.writeTree());
		assertEquals(2, SkillTrees.rankOf(fresh.progress().ranks, SkillTrees.VIGOR_NODE));

		Map<String, SkillTrees.Progress> two = new LinkedHashMap<>();
		two.put("infusedclasses:warrior", classes.progress());
		two.put("infusedclasses:ranger", new SkillTrees.Progress());
		Map<String, SkillTrees.Progress> decoded = SkillTrees.decodeMap(SkillTrees.encodeMap(two));
		assertEquals(2, decoded.get("infusedclasses:warrior").ranks.get(SkillTrees.VIGOR_NODE));
		assertTrue(decoded.get("infusedclasses:ranger").ranks.isEmpty());
	}

	@Test
	void legacyIdsFoldIntoRanksAndUnknownsDrop() {
		SkillTrees.Progress p = SkillTrees.decodeProgress(
				"5,10,20,juggernaut_vigor1+juggernaut_vigor2+juggernaut_plate1+juggernaut_plate2"
						+ "+juggernaut_ward2+juggernaut_plate3+juggernaut_ward3+juggernaut_hide1"
						+ "+juggernaut_war_cry_potency+bogus");
		assertEquals(2, SkillTrees.rankOf(p.ranks, SkillTrees.VIGOR_NODE));
		assertEquals(5, SkillTrees.rankOf(p.ranks, SkillTrees.BULWARK_NODE));
		assertEquals(1, SkillTrees.rankOf(p.ranks, SkillTrees.HIDE_NODE));
		assertTrue(p.ranks.keySet().stream().allMatch(id -> SkillTrees.byId(id) != null));

		SkillTrees.Progress over = SkillTrees.decodeProgress("0,1,0,juggernaut_vigor:9");
		assertEquals(5, SkillTrees.rankOf(over.ranks, SkillTrees.VIGOR_NODE));
		assertEquals(25, SkillTrees.decodeProgress("0,25,3,").level);
	}

	@Test
	void malformedTreeDecodesToDefaults() {
		assertEquals(1, SkillTrees.decodeProgress("nope").level);
		assertEquals(1, SkillTrees.decodeProgress(null).level);
		assertTrue(SkillTrees.decodeMap("junk").isEmpty());
		assertNull(SkillTrees.byId("juggernaut_bard"));
	}

	@Test
	void respawnCopyKeepsRanksButWipesStacks() {
		PlayerClasses oldClasses = warrior();
		oldClasses.progress().unspent = 5;
		oldClasses.tryUnlock(SkillTrees.VIGOR_NODE);
		oldClasses.tryUnlock(SkillTrees.THICK_SKIN_NODE);
		oldClasses.thickSkin().hit(1_000L);

		PlayerClasses fresh = new PlayerClasses();
		fresh.copyFrom(oldClasses);

		assertEquals(ClassRegistry.WARRIOR, fresh.classId());
		assertEquals(1, SkillTrees.rankOf(fresh.progress().ranks, SkillTrees.THICK_SKIN_NODE));
		assertEquals(0, fresh.thickSkin().count());
	}

	@Test
	void describeNamesStateForUi() {
		SkillTrees.Node vigor = SkillTrees.byId(SkillTrees.VIGOR_NODE);
		assertEquals("Available", SkillTrees.describe(new SkillTrees.Check.Ok(), vigor));
		assertEquals("Rank 5/5", SkillTrees.describe(new SkillTrees.Check.Maxed(), vigor));
		assertEquals("Unknown talent", SkillTrees.describe(new SkillTrees.Check.Unknown(), null));
		assertTrue(SkillTrees.describe(
				new SkillTrees.Check.TierLocked(2, 5, 0), vigor).contains("5"));
		assertTrue(SkillTrees.describe(
				new SkillTrees.Check.MissingParents(List.of("Thick Skin")), vigor).contains("Thick Skin"));
	}
}
