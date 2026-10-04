package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Juggernaut progression. Pure: XP curve, ranked node data, tier spend plus
 * keystone chain, rank-map persistence with legacy folding, Thick Skin stacks,
 * War Cry upgrades, and the shout cooldown value. No live player needed, so
 * tests assert here and the server entry only maps to effects.
 */
public final class SkillTrees {
	/** Knee of the XP curve, not a cap: levels above it cost the level-20 price. */
	public static final int MAX_LEVEL = 20;
	public static final int BASE_KILL_XP = 8;
	public static final int MELEE_BONUS_XP = 7;
	public static final int BASE_LEVEL_XP = 22;
	public static final double LEVEL_GROWTH = 1.25;

	/** Points spent in-tree required per tier. Ranked lines run five deep, so one maxed line opens the next tier. */
	public static final Map<Integer, Integer> TIER_REQ = Map.of(1, 0, 2, 5, 3, 10, 4, 12);

	public static final String VIGOR_NODE = "juggernaut_vigor";
	public static final String BULWARK_NODE = "juggernaut_bulwark";
	public static final String HIDE_NODE = "juggernaut_hide";
	public static final String THICK_SKIN_NODE = "juggernaut_thick_skin";
	public static final String WAR_CRY_NODE = "juggernaut_war_cry";
	public static final String ENDURING_SKIN_NODE = "juggernaut_enduring_skin";
	public static final String RALLYING_NODE = "juggernaut_rallying_roar";

	public static final int THICK_SKIN_CAP = 5;
	public static final double THICK_SKIN_ARMOR_PER_STACK = 1.0;
	public static final long THICK_SKIN_DECAY_MS = 5_000L;
	// ponytail: capstone only reparametrizes the stack engine. Higher cap + longer decay, no new trigger.
	public static final int ENDURING_SKIN_CAP = 8;
	public static final long ENDURING_SKIN_DECAY_MS = 10_000L;
	public static final Identifier THICK_SKIN_MODIFIER =
			Identifier.fromNamespaceAndPath("infusedclasses", "thick_skin");
	public static final Identifier THICK_SKIN_ATTRIBUTE =
			Identifier.fromNamespaceAndPath("minecraft", "armor");

	public static final int WAR_CRY_UPGRADED_TICKS = 240; // 12s
	public static final int WAR_CRY_BASE_AMP = 1;
	public static final long RALLYING_COOLDOWN_MS = 20_000L;

	/** Branches visible but locked: no nodes, no spend, placeholders only. */
	public static final List<String> LOCKED_BRANCHES = List.of("Berserker");

	/**
	 * A tree node. Ranked stat lines carry maxRank plus an optional alternating
	 * bonus (Bulwark: armor on odd ranks, toughness on even). Null bonus means
	 * a keystone or upgrade with no attribute of its own. Parents are only
	 * used for attached upgrades; all other gating is tier spend. Col and row
	 * place the slot on the panel grid; icon names a vanilla item id.
	 */
	public record Node(String id, String name, String blurb, int cost, int maxRank, Passive bonus,
			Passive bonusAlt, List<String> parents, int tier, int col, int row, Identifier icon) {
	}

	public record WarCryEffects(int strengthTicks, int strengthAmp, boolean resistance, int resistTicks,
			int resistAmp, boolean absorption) {
	}

	/** Unlock verdict. Pure data; the screen and server format it at the edges. */
	public sealed interface Check permits Check.Ok, Check.Unknown, Check.Maxed, Check.NeedPoints,
			Check.TierLocked, Check.MissingParents {
		record Ok() implements Check {
		}

		record Unknown() implements Check {
		}

		record Maxed() implements Check {
		}

		record NeedPoints(int need, int have) implements Check {
		}

		record TierLocked(int tier, int need, int have) implements Check {
		}

		record MissingParents(List<String> missing) implements Check {
		}
	}

	/** Mutable per-class progress. Level starts at 1, each level-up banks one point, ranks map node to taken ranks. */
	public static final class Progress {
		public int xp;
		public int level = 1;
		public int unspent;
		public final Map<String, Integer> ranks = new LinkedHashMap<>();
	}

	/** Transient Thick Skin stacks: in-memory only, capped, wiped on death/logout by never persisting. */
	public static final class ThickSkin {
		private int stacks;
		private long lastHit;

		public int count() {
			return stacks;
		}

		public int hit(long now) {
			return hit(now, THICK_SKIN_CAP);
		}

		public int hit(long now, int cap) {
			lastHit = now;
			if (stacks < cap) {
				stacks++;
			}
			return stacks;
		}

		public int tick(long now) {
			return tick(now, THICK_SKIN_DECAY_MS);
		}

		public int tick(long now, long decayMs) {
			if (stacks > 0 && now - lastHit > decayMs) {
				stacks = 0;
			}
			return stacks;
		}

		public void clear() {
			stacks = 0;
			lastHit = 0;
		}
	}

	public static boolean enduringUnlocked(Set<String> unlocked) {
		return unlocked != null && unlocked.contains(ENDURING_SKIN_NODE);
	}

	public static int capFor(Set<String> unlocked) {
		return enduringUnlocked(unlocked) ? ENDURING_SKIN_CAP : THICK_SKIN_CAP;
	}

	public static long decayFor(Set<String> unlocked) {
		return enduringUnlocked(unlocked) ? ENDURING_SKIN_DECAY_MS : THICK_SKIN_DECAY_MS;
	}

	public static final List<Node> NODES = List.of(
			new Node(VIGOR_NODE, "Vigor", "+1 Max Health per rank", 1, 5,
					new Passive(minecraftId("max_health"), treeModifier("jug_vigor"), 1.0, AttributeModifier.Operation.ADD_VALUE),
					null, List.of(), 1, 0, 0, minecraftId("apple")),
			new Node(THICK_SKIN_NODE, "Thick Skin", "Hits grant +1 Armor, up to 5, lost out of combat", 1, 1,
					null, null, List.of(), 1, 1, 0, minecraftId("turtle_helmet")),
			new Node(ENDURING_SKIN_NODE, "Enduring Skin", "Thick Skin holds 8 stacks and lingers 10s out of combat", 1, 1,
					null, null, List.of(THICK_SKIN_NODE), 3, 2, 2, minecraftId("totem_of_undying")),
			new Node(BULWARK_NODE, "Bulwark", "+1 Armor / +0.5 Toughness alternating per rank", 1, 5,
					new Passive(minecraftId("armor"), treeModifier("jug_bulwark"), 1.0, AttributeModifier.Operation.ADD_VALUE),
					new Passive(minecraftId("armor_toughness"), treeModifier("jug_bulwark_ward"), 0.5, AttributeModifier.Operation.ADD_VALUE),
					List.of(), 2, 0, 1, minecraftId("iron_chestplate")),
			new Node(WAR_CRY_NODE, "Hold the Line", "War Cry also grants Resistance, lasts 12s", 2, 1,
					null, null, List.of(THICK_SKIN_NODE), 2, 1, 1, minecraftId("goat_horn")),
			new Node(HIDE_NODE, "Hide", "+5% Knockback Resist per rank", 1, 5,
					new Passive(minecraftId("knockback_resistance"), treeModifier("jug_hide"), 0.05, AttributeModifier.Operation.ADD_VALUE),
					null, List.of(), 3, 0, 2, minecraftId("shield")),
			new Node(RALLYING_NODE, "Rallying Roar", "War Cry grants Absorption, recharges in 20s", 1, 1,
					null, null, List.of(WAR_CRY_NODE), 4, 1, 3, minecraftId("netherite_chestplate")));

	/** Old one-shot ids fold into ranked lines, capped at the line max. Anything else drops. */
	private static final Map<String, String> LEGACY_FOLD = Map.ofEntries(
			Map.entry("juggernaut_vigor1", VIGOR_NODE),
			Map.entry("juggernaut_vigor2", VIGOR_NODE),
			Map.entry("juggernaut_vigor3", VIGOR_NODE),
			Map.entry("juggernaut_vigor4", VIGOR_NODE),
			Map.entry("juggernaut_plate1", BULWARK_NODE),
			Map.entry("juggernaut_plate2", BULWARK_NODE),
			Map.entry("juggernaut_plate3", BULWARK_NODE),
			Map.entry("juggernaut_ward2", BULWARK_NODE),
			Map.entry("juggernaut_ward3", BULWARK_NODE),
			Map.entry("juggernaut_hide1", HIDE_NODE),
			Map.entry("juggernaut_hide2", HIDE_NODE),
			Map.entry("juggernaut_hide3", HIDE_NODE),
			Map.entry(THICK_SKIN_NODE, THICK_SKIN_NODE),
			Map.entry(WAR_CRY_NODE, WAR_CRY_NODE),
			Map.entry(ENDURING_SKIN_NODE, ENDURING_SKIN_NODE));

	static {
		for (Node n : NODES) {
			if (n.bonus() != null && !ClassAttributes.isKnown(n.bonus().attribute())) {
				throw new IllegalArgumentException("Unknown attribute " + n.bonus().attribute() + " for node " + n.id());
			}
			if (n.bonusAlt() != null && !ClassAttributes.isKnown(n.bonusAlt().attribute())) {
				throw new IllegalArgumentException("Unknown attribute " + n.bonusAlt().attribute() + " for node " + n.id());
			}
			for (String parent : n.parents()) {
				if (byId(parent) == null) {
					throw new IllegalArgumentException("Unknown parent " + parent + " for node " + n.id());
				}
			}
			if (!TIER_REQ.containsKey(n.tier())) {
				throw new IllegalArgumentException("Unknown tier " + n.tier() + " for node " + n.id());
			}
		}
	}

	private SkillTrees() {
	}

	private static Identifier minecraftId(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
	}

	private static Identifier treeModifier(String suffix) {
		return Identifier.fromNamespaceAndPath("infusedclasses", suffix);
	}

	public static Node byId(String id) {
		if (id == null) {
			return null;
		}
		for (Node n : NODES) {
			if (n.id().equals(id)) {
				return n;
			}
		}
		return null;
	}

	/** XP to leave `level` for `level + 1`. Exponential through 20, flat ceiling beyond so play keeps earning. */
	public static int xpForNextLevel(int level) {
		if (level < 1) {
			return Integer.MAX_VALUE;
		}
		int effective = Math.min(level, MAX_LEVEL);
		return (int) Math.round(BASE_LEVEL_XP * Math.pow(LEVEL_GROWTH, effective - 1));
	}

	public static int xpForKill(boolean meleeAffinity) {
		return BASE_KILL_XP + (meleeAffinity ? MELEE_BONUS_XP : 0);
	}

	public static int rankOf(Map<String, Integer> ranks, String id) {
		if (ranks == null || id == null) {
			return 0;
		}
		return Math.max(0, ranks.getOrDefault(id, 0));
	}

	public static int spentPoints(Map<String, Integer> ranks) {
		int spent = 0;
		if (ranks == null) {
			return 0;
		}
		for (Map.Entry<String, Integer> e : ranks.entrySet()) {
			Node n = byId(e.getKey());
			if (n != null && e.getValue() > 0) {
				spent += n.cost() * e.getValue();
			}
		}
		return spent;
	}

	public static Check check(Map<String, Integer> ranks, int unspent, String nodeId) {
		Node node = byId(nodeId);
		if (node == null) {
			return new Check.Unknown();
		}
		if (rankOf(ranks, nodeId) >= node.maxRank()) {
			return new Check.Maxed();
		}
		if (unspent < node.cost()) {
			return new Check.NeedPoints(node.cost(), unspent);
		}
		int need = TIER_REQ.get(node.tier());
		int have = spentPoints(ranks);
		if (have < need) {
			return new Check.TierLocked(node.tier(), need, have);
		}
		List<String> missing = new ArrayList<>();
		for (String parent : node.parents()) {
			if (rankOf(ranks, parent) < 1) {
				Node p = byId(parent);
				missing.add(p == null ? parent : p.name());
			}
		}
		if (!missing.isEmpty()) {
			return new Check.MissingParents(List.copyOf(missing));
		}
		return new Check.Ok();
	}

	/** English rendering of a verdict for tooltips and chat. Pure formatting at the edge. */
	public static String describe(Check check, Node node) {
		String name = node == null ? "talent" : node.name();
		if (check instanceof Check.Ok) {
			return "Available";
		}
		if (check instanceof Check.Unknown) {
			return "Unknown talent";
		}
		if (check instanceof Check.Maxed) {
			return "Rank " + (node == null ? "maxed" : node.maxRank() + "/" + node.maxRank());
		}
		if (check instanceof Check.NeedPoints need) {
			return "Need " + need.need() + " point" + (need.need() == 1 ? "" : "s");
		}
		if (check instanceof Check.TierLocked tier) {
			return "Requires " + tier.need() + " spent (tier " + tier.tier() + ")";
		}
		if (check instanceof Check.MissingParents missing) {
			return "Needs " + String.join(", ", missing.missing()) + " first";
		}
		return "Unavailable";
	}

	public static boolean canUnlock(Map<String, Integer> ranks, int unspent, String nodeId) {
		return check(ranks, unspent, nodeId) instanceof Check.Ok;
	}

	/** Rank-scaled passives for owned ranks, for the existing modifier path. Alternating lines split across two modifiers. */
	public static List<Passive> bonusesFor(Map<String, Integer> ranks) {
		List<Passive> out = new ArrayList<>();
		if (ranks == null) {
			return out;
		}
		for (Map.Entry<String, Integer> e : ranks.entrySet()) {
			Node n = byId(e.getKey());
			int rank = e.getValue() == null ? 0 : e.getValue();
			if (n == null || n.bonus() == null || rank < 1) {
				continue;
			}
			if (n.bonusAlt() == null) {
				out.add(new Passive(n.bonus().attribute(), n.bonus().modifier(), n.bonus().value() * rank, n.bonus().operation()));
			} else {
				int odd = (rank + 1) / 2;
				int even = rank / 2;
				out.add(new Passive(n.bonus().attribute(), n.bonus().modifier(), n.bonus().value() * odd, n.bonus().operation()));
				if (even > 0) {
					out.add(new Passive(n.bonusAlt().attribute(), n.bonusAlt().modifier(),
							n.bonusAlt().value() * even, n.bonusAlt().operation()));
				}
			}
		}
		return out;
	}

	public static boolean warCryUpgraded(Set<String> unlocked) {
		return unlocked != null && unlocked.contains(WAR_CRY_NODE);
	}

	public static boolean rallyingOwned(Set<String> unlocked) {
		return unlocked != null && unlocked.contains(RALLYING_NODE);
	}

	public static boolean thickSkinUnlocked(Set<String> unlocked) {
		return unlocked != null && unlocked.contains(THICK_SKIN_NODE);
	}

	public static long warCryCooldownMs(Set<String> unlocked) {
		return rallyingOwned(unlocked) ? RALLYING_COOLDOWN_MS : Cooldowns.WAR_CRY_MS;
	}

	public static WarCryEffects warCryEffects(boolean upgraded) {
		if (upgraded) {
			return new WarCryEffects(WAR_CRY_UPGRADED_TICKS, 1, true, WAR_CRY_UPGRADED_TICKS, 0, false);
		}
		return new WarCryEffects(Cooldowns.WAR_CRY_EFFECT_TICKS, 1, false, 0, 0, false);
	}

	/** Hold the Line adds duration plus Resistance; Rallying Roar adds Absorption on top. */
	public static WarCryEffects warCryEffects(Set<String> unlocked) {
		if (rallyingOwned(unlocked)) {
			return new WarCryEffects(WAR_CRY_UPGRADED_TICKS, 1, true, WAR_CRY_UPGRADED_TICKS, 0, true);
		}
		return warCryEffects(unlocked != null && unlocked.contains(WAR_CRY_NODE));
	}

	/** Single progress as "xp,level,unspent,id:rank+id:rank". */
	public static String encodeProgress(Progress p) {
		StringBuilder out = new StringBuilder();
		out.append(p.xp).append(',').append(p.level).append(',').append(p.unspent).append(',');
		boolean first = true;
		for (Map.Entry<String, Integer> e : p.ranks.entrySet()) {
			Node n = byId(e.getKey());
			int rank = e.getValue() == null ? 0 : e.getValue();
			if (n == null || rank < 1) {
				continue;
			}
			if (!first) {
				out.append('+');
			}
			out.append(e.getKey()).append(':').append(Math.min(rank, n.maxRank()));
			first = false;
		}
		return out.toString();
	}

	public static Progress decodeProgress(String s) {
		Progress p = new Progress();
		if (s == null || s.isEmpty()) {
			return p;
		}
		String[] parts = s.split(",", -1);
		try {
			if (parts.length > 0 && !parts[0].isEmpty()) {
				p.xp = Math.max(0, Integer.parseInt(parts[0]));
			}
			if (parts.length > 1 && !parts[1].isEmpty()) {
				p.level = Math.max(1, Integer.parseInt(parts[1]));
			}
			if (parts.length > 2 && !parts[2].isEmpty()) {
				p.unspent = Math.max(0, Integer.parseInt(parts[2]));
			}
		} catch (NumberFormatException bad) {
			return new Progress();
		}
		if (parts.length > 3 && !parts[3].isEmpty()) {
			for (String token : parts[3].split("\\+", -1)) {
				String id = token;
				int rank = 1;
				int colon = token.indexOf(':');
				if (colon >= 0) {
					id = token.substring(0, colon);
					try {
						rank = Integer.parseInt(token.substring(colon + 1));
					} catch (NumberFormatException bad) {
						continue;
					}
				}
				String folded = LEGACY_FOLD.getOrDefault(id, id);
				Node n = byId(folded);
				if (n == null) {
					continue;
				}
				p.ranks.merge(folded, Math.max(1, rank), Integer::sum);
			}
			for (Map.Entry<String, Integer> e : List.copyOf(p.ranks.entrySet())) {
				Node n = byId(e.getKey());
				if (n == null) {
					p.ranks.remove(e.getKey());
				} else {
					p.ranks.put(e.getKey(), Math.min(Math.max(1, e.getValue()), n.maxRank()));
				}
			}
		}
		return p;
	}

	/** Per-class map as "classId=xp,level,unspent,ranks;...". */
	public static String encodeMap(Map<String, Progress> map) {
		StringBuilder out = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, Progress> e : map.entrySet()) {
			if (!first) {
				out.append(';');
			}
			out.append(e.getKey()).append('=').append(encodeProgress(e.getValue()));
			first = false;
		}
		return out.toString();
	}

	public static Map<String, Progress> decodeMap(String s) {
		Map<String, Progress> out = new LinkedHashMap<>();
		if (s == null || s.isEmpty()) {
			return out;
		}
		for (String entry : s.split(";", -1)) {
			int eq = entry.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = entry.substring(0, eq);
			if (Identifier.tryParse(key) == null) {
				continue;
			}
			out.put(key, decodeProgress(entry.substring(eq + 1)));
		}
		return out;
	}
}
