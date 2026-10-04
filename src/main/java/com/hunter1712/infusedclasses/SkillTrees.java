package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Juggernaut progression. Pure: XP curve, node data, gates, spend, respec,
 * Thick Skin stacks, War Cry upgrade, and string persistence. No live player
 * needed, so tests assert here and the server entry only maps to effects.
 */
public final class SkillTrees {
	public static final int MAX_LEVEL = 20;
	public static final int BASE_KILL_XP = 8;
	public static final int MELEE_BONUS_XP = 7;
	public static final int BASE_LEVEL_XP = 15;
	public static final double LEVEL_GROWTH = 1.25;

	public static final String THICK_SKIN_NODE = "juggernaut_thick_skin";
	public static final String WAR_CRY_NODE = "juggernaut_war_cry";
	public static final String WAR_CRY_POTENCY_NODE = "juggernaut_war_cry_potency";

	public static final int THICK_SKIN_CAP = 5;
	public static final double THICK_SKIN_ARMOR_PER_STACK = 1.0;
	public static final long THICK_SKIN_DECAY_MS = 5_000L;
	public static final Identifier THICK_SKIN_MODIFIER =
			Identifier.fromNamespaceAndPath("infusedclasses", "thick_skin");
	public static final Identifier THICK_SKIN_ATTRIBUTE =
			Identifier.fromNamespaceAndPath("minecraft", "armor");

	public static final int WAR_CRY_UPGRADED_TICKS = 240; // 12s
	public static final int WAR_CRY_POTENCY_TICKS = Cooldowns.WAR_CRY_EFFECT_TICKS; // 8s, harder not longer
	public static final int WAR_CRY_BASE_AMP = 1;
	public static final int WAR_CRY_POTENCY_AMP = 2;

	/** Branches visible but locked in MVP: no nodes, no spend, placeholders only. */
	public static final List<String> LOCKED_BRANCHES = List.of("Berserker", "Warlord");

	/** A tree node: cost in points, gate as points already spent in-branch. Null bonus = special (stacks/ability). */
	public record Node(String id, String name, String blurb, int cost, int gate, Passive bonus) {
	}

	public record WarCryEffects(int strengthTicks, int strengthAmp, boolean resistance, int resistTicks, int resistAmp) {
	}

	/** Mutable per-class progress. Level starts at 1, each level-up banks one point. */
	public static final class Progress {
		public int xp;
		public int level = 1;
		public int unspent;
		public final Set<String> unlocked = new LinkedHashSet<>();
	}

	/** Transient Thick Skin stacks: in-memory only, capped, wiped on death/logout by never persisting. */
	public static final class ThickSkin {
		private int stacks;
		private long lastHit;

		public int count() {
			return stacks;
		}

		public int hit(long now) {
			lastHit = now;
			if (stacks < THICK_SKIN_CAP) {
				stacks++;
			}
			return stacks;
		}

		public int tick(long now) {
			if (stacks > 0 && now - lastHit > THICK_SKIN_DECAY_MS) {
				stacks = 0;
			}
			return stacks;
		}

		public void clear() {
			stacks = 0;
			lastHit = 0;
		}
	}

	public static final List<Node> NODES = List.of(
			new Node("juggernaut_vigor1", "Vigor I", "+2 Max Health", 1, 0,
					new Passive(minecraftId("max_health"), treeModifier("jug_vigor1"), 2.0, AttributeModifier.Operation.ADD_VALUE)),
			new Node("juggernaut_vigor2", "Vigor II", "+2 Max Health", 1, 1,
					new Passive(minecraftId("max_health"), treeModifier("jug_vigor2"), 2.0, AttributeModifier.Operation.ADD_VALUE)),
			new Node("juggernaut_plate1", "Plate I", "+1 Armor", 1, 1,
					new Passive(minecraftId("armor"), treeModifier("jug_plate1"), 1.0, AttributeModifier.Operation.ADD_VALUE)),
			new Node("juggernaut_plate2", "Plate II", "+1 Toughness", 1, 2,
					new Passive(minecraftId("armor_toughness"), treeModifier("jug_plate2"), 1.0, AttributeModifier.Operation.ADD_VALUE)),
			new Node(THICK_SKIN_NODE, "Thick Skin", "Hits grant +1 Armor, up to 5, lost out of combat", 1, 2, null),
			new Node(WAR_CRY_NODE, "Hold the Line", "War Cry also grants Resistance, lasts 12s", 2, 4, null),
			new Node(WAR_CRY_POTENCY_NODE, "Unbreakable Shout", "War Cry Strength II plus Resistance, 8s. One War Cry path only", 2, 4, null));

	static {
		for (Node n : NODES) {
			if (n.bonus() != null && !ClassAttributes.isKnown(n.bonus().attribute())) {
				throw new IllegalArgumentException("Unknown attribute " + n.bonus().attribute() + " for node " + n.id());
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

	/** XP to leave `level` for `level + 1`. Cap returns never-reachable. */
	public static int xpForNextLevel(int level) {
		if (level < 1 || level >= MAX_LEVEL) {
			return Integer.MAX_VALUE;
		}
		return (int) Math.round(BASE_LEVEL_XP * Math.pow(LEVEL_GROWTH, level - 1));
	}

	public static int xpForKill(boolean meleeAffinity) {
		return BASE_KILL_XP + (meleeAffinity ? MELEE_BONUS_XP : 0);
	}

	public static int spentPoints(Set<String> unlocked) {
		int spent = 0;
		if (unlocked == null) {
			return 0;
		}
		for (String id : unlocked) {
			Node n = byId(id);
			if (n != null) {
				spent += n.cost();
			}
		}
		return spent;
	}

	/** Null when the unlock is legal; otherwise a short English reason for chat. */
	public static String unlockRejectReason(Set<String> unlocked, int unspent, String nodeId) {
		Node node = byId(nodeId);
		if (node == null) {
			return "Unknown talent";
		}
		if (unlocked != null && unlocked.contains(nodeId)) {
			return "Already unlocked";
		}
		if (unspent < node.cost()) {
			return "Need " + node.cost() + " point" + (node.cost() == 1 ? "" : "s");
		}
		if (spentPoints(unlocked) < node.gate()) {
			return "Requires " + node.gate() + " spent first";
		}
		if (isWarCryNode(nodeId) && unlocked != null) {
			for (String other : unlocked) {
				if (!other.equals(nodeId) && isWarCryNode(other)) {
					return "Choose one War Cry path (respec to switch)";
				}
			}
		}
		return null;
	}

	public static boolean canUnlock(Set<String> unlocked, int unspent, String nodeId) {
		return unlockRejectReason(unlocked, unspent, nodeId) == null;
	}

	/** Small-node passives for currently unlocked nodes, for the existing modifier path. */
	public static List<Passive> bonusesFor(Set<String> unlocked) {
		List<Passive> out = new ArrayList<>();
		if (unlocked == null) {
			return out;
		}
		for (String id : unlocked) {
			Node n = byId(id);
			if (n != null && n.bonus() != null) {
				out.add(n.bonus());
			}
		}
		return out;
	}

	public static boolean warCryUpgraded(Set<String> unlocked) {
		return unlocked != null && (unlocked.contains(WAR_CRY_NODE) || unlocked.contains(WAR_CRY_POTENCY_NODE));
	}

	public static boolean isWarCryNode(String nodeId) {
		return WAR_CRY_NODE.equals(nodeId) || WAR_CRY_POTENCY_NODE.equals(nodeId);
	}

	public static boolean thickSkinUnlocked(Set<String> unlocked) {
		return unlocked != null && unlocked.contains(THICK_SKIN_NODE);
	}

	public static WarCryEffects warCryEffects(boolean upgraded) {
		if (upgraded) {
			return new WarCryEffects(WAR_CRY_UPGRADED_TICKS, 1, true, WAR_CRY_UPGRADED_TICKS, 0);
		}
		return new WarCryEffects(Cooldowns.WAR_CRY_EFFECT_TICKS, 1, false, 0, 0);
	}

	/** Set-based effects: duration path lasts longer, potency path hits harder, either adds Resistance. */
	public static WarCryEffects warCryEffects(Set<String> unlocked) {
		if (unlocked != null && unlocked.contains(WAR_CRY_POTENCY_NODE)) {
			return new WarCryEffects(WAR_CRY_POTENCY_TICKS, WAR_CRY_POTENCY_AMP, true, WAR_CRY_POTENCY_TICKS, 0);
		}
		return warCryEffects(unlocked != null && unlocked.contains(WAR_CRY_NODE));
	}

	/** Single progress as "xp,level,unspent,node+node". */
	public static String encodeProgress(Progress p) {
		StringBuilder out = new StringBuilder();
		out.append(p.xp).append(',').append(p.level).append(',').append(p.unspent).append(',');
		boolean first = true;
		for (String id : p.unlocked) {
			if (byId(id) == null) {
				continue;
			}
			if (!first) {
				out.append('+');
			}
			out.append(id);
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
				p.level = Math.min(MAX_LEVEL, Math.max(1, Integer.parseInt(parts[1])));
			}
			if (parts.length > 2 && !parts[2].isEmpty()) {
				p.unspent = Math.max(0, Integer.parseInt(parts[2]));
			}
		} catch (NumberFormatException bad) {
			return new Progress();
		}
		if (parts.length > 3 && !parts[3].isEmpty()) {
			for (String id : parts[3].split("\\+", -1)) {
				if (byId(id) != null) {
					p.unlocked.add(id);
				}
			}
		}
		return p;
	}

	/** Per-class map as "classId=xp,level,unspent,nodes;...". */
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
