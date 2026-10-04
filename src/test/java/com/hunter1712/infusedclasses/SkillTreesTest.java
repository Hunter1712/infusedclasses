package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreesTest {
	private static PlayerClasses warrior() {
		PlayerClasses classes = new PlayerClasses();
		classes.trySetClass(ClassRegistry.WARRIOR);
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
		assertEquals(Integer.MAX_VALUE, SkillTrees.xpForNextLevel(SkillTrees.MAX_LEVEL));
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
	void capstoneGatedDoubleSpendRejectedRespecRefunds() {
		PlayerClasses classes = warrior();
		classes.progress().unspent = 10;

		assertNotNull(SkillTrees.unlockRejectReason(classes.unlockedNodes(), classes.unspentPoints(), SkillTrees.WAR_CRY_NODE));
		assertFalse(classes.tryUnlock(SkillTrees.WAR_CRY_NODE));

		assertTrue(classes.tryUnlock("juggernaut_vigor1"));
		assertFalse(classes.tryUnlock("juggernaut_vigor1"));
		assertEquals(9, classes.unspentPoints());

		int refund = classes.respec();
		assertEquals(1, refund);
		assertEquals(10, classes.unspentPoints());
		assertTrue(classes.unlockedNodes().isEmpty());
	}

	@Test
	void warCryForkIsDurationVsPotencyAndMutuallyExclusive() {
		SkillTrees.WarCryEffects base = SkillTrees.warCryEffects(java.util.Set.of());
		SkillTrees.WarCryEffects duration = SkillTrees.warCryEffects(java.util.Set.of(SkillTrees.WAR_CRY_NODE));
		SkillTrees.WarCryEffects potency = SkillTrees.warCryEffects(java.util.Set.of(SkillTrees.WAR_CRY_POTENCY_NODE));

		assertTrue(duration.resistance());
		assertTrue(potency.resistance());
		assertTrue(duration.strengthTicks() > base.strengthTicks());
		assertEquals(Cooldowns.WAR_CRY_EFFECT_TICKS, potency.strengthTicks());
		assertTrue(potency.strengthAmp() > duration.strengthAmp());

		PlayerClasses classes = warrior();
		classes.progress().unspent = 10;
		assertTrue(classes.tryUnlock("juggernaut_vigor1"));
		assertTrue(classes.tryUnlock("juggernaut_vigor2"));
		assertTrue(classes.tryUnlock("juggernaut_plate1"));
		assertTrue(classes.tryUnlock("juggernaut_plate2"));
		assertTrue(classes.tryUnlock(SkillTrees.WAR_CRY_NODE));
		assertFalse(classes.tryUnlock(SkillTrees.WAR_CRY_POTENCY_NODE));
		assertNotNull(SkillTrees.unlockRejectReason(classes.unlockedNodes(), classes.unspentPoints(), SkillTrees.WAR_CRY_POTENCY_NODE));
	}

	@Test
	void nonWarriorCannotUnlockJuggernautNodes() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:ranger");
		classes.progress().unspent = 10;
		assertFalse(classes.tryUnlock("juggernaut_vigor1"));

		PlayerClasses classless = new PlayerClasses();
		assertEquals(0, classless.addKillXp(false));
		assertFalse(classless.tryUnlock("juggernaut_vigor1"));
	}

	@Test
	void lockedBranchesAreDeclared() {
		assertTrue(SkillTrees.LOCKED_BRANCHES.contains("Berserker"));
		assertTrue(SkillTrees.LOCKED_BRANCHES.contains("Warlord"));
	}

	@Test
	void rawXpGrantsLevelsAndCapsAtTwenty() {
		PlayerClasses classes = warrior();
		int gained = classes.addXp(100000);
		assertEquals(SkillTrees.MAX_LEVEL, classes.level());
		assertEquals(SkillTrees.MAX_LEVEL - 1, classes.unspentPoints());
		assertTrue(gained > 0);
		assertEquals(0, classes.addXp(100000));
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
	void warCryUpgradeAddsResistanceAndDuration() {
		SkillTrees.WarCryEffects base = SkillTrees.warCryEffects(false);
		SkillTrees.WarCryEffects upgraded = SkillTrees.warCryEffects(true);

		assertFalse(base.resistance());
		assertEquals(Cooldowns.WAR_CRY_EFFECT_TICKS, base.strengthTicks());
		assertTrue(upgraded.resistance());
		assertTrue(upgraded.strengthTicks() > base.strengthTicks());
	}

	@Test
	void progressRoundTripsAndStaysPerClass() {
		PlayerClasses classes = warrior();
		classes.progress().unspent = 3;
		classes.tryUnlock("juggernaut_vigor1");

		Map<String, SkillTrees.Progress> back = SkillTrees.decodeMap(SkillTrees.encodeMap(Map.of(
				ClassRegistry.WARRIOR.toString(), classes.progress())));
		assertTrue(back.get(ClassRegistry.WARRIOR.toString()).unlocked.contains("juggernaut_vigor1"));

		PlayerClasses fresh = new PlayerClasses();
		fresh.readId("infusedclasses:warrior");
		fresh.readTree(classes.writeTree());
		assertTrue(fresh.unlockedNodes().contains("juggernaut_vigor1"));

		Map<String, SkillTrees.Progress> two = new LinkedHashMap<>();
		two.put("infusedclasses:warrior", classes.progress());
		two.put("infusedclasses:ranger", new SkillTrees.Progress());
		Map<String, SkillTrees.Progress> decoded = SkillTrees.decodeMap(SkillTrees.encodeMap(two));
		assertEquals(1, decoded.get("infusedclasses:warrior").unlocked.size());
		assertTrue(decoded.get("infusedclasses:ranger").unlocked.isEmpty());
	}

	@Test
	void malformedTreeDecodesToDefaults() {
		assertEquals(1, SkillTrees.decodeProgress("nope").level);
		assertEquals(1, SkillTrees.decodeProgress(null).level);
		assertTrue(SkillTrees.decodeMap("junk").isEmpty());
		assertNull(SkillTrees.byId("juggernaut_bard"));
	}

	@Test
	void respawnCopyKeepsUnlocksButWipesStacks() {
		PlayerClasses oldClasses = warrior();
		oldClasses.progress().unspent = 5;
		oldClasses.tryUnlock("juggernaut_vigor1");
		oldClasses.tryUnlock("juggernaut_plate1");
		oldClasses.tryUnlock(SkillTrees.THICK_SKIN_NODE);
		oldClasses.thickSkin().hit(1_000L);

		PlayerClasses fresh = new PlayerClasses();
		fresh.copyFrom(oldClasses);

		assertEquals(ClassRegistry.WARRIOR, fresh.classId());
		assertTrue(fresh.unlockedNodes().contains(SkillTrees.THICK_SKIN_NODE));
		assertEquals(0, fresh.thickSkin().count());
	}
}
