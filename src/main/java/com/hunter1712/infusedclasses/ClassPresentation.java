package com.hunter1712.infusedclasses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure presentation for class cards and tooltips. One seam for all renderers.
 */
public final class ClassPresentation {
	private static final int HEADER_COLOR = 0xFFFFFFFF;   // white for section headers
	private static final int PASSIVE_COLOR = 0xFFBDBDBD;   // light gray for passive items
	private static final int ABILITY_COLOR = 0xFF7ED6FF;   // blue for ability items
	private static final int NOTICE_COLOR = 0xFFE8C15A;    // title gold for the coming-soon notice
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int HOVER_BORDER_COLOR = 0xFF8A8A8A;
	private static final int HOVER_WASH_COLOR = 0x14FFFFFF;
	private static final int LOCKED_VEIL_COLOR = 0x50000000;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int NAME_LOCKED_COLOR = 0xFF8A8A8A;

	private ClassPresentation() {
	}

	/** One rendered card line plus which section it belongs to. */
	public enum LineKind {
		PASSIVE_HEADER, PASSIVE, ABILITY_HEADER, ABILITY, NOTICE
	}

	/** The colour and weight a line draws with. Decided by line kind, never by its text. */
	public record LineStyle(int color, boolean bold) {
	}

	/** One rendered card line, the section it belongs to, and the style it draws with. */
	public record CardLine(String text, LineKind kind, LineStyle style) {
		/** A line's style comes from its kind, so a renderer never has to read the line. */
		public CardLine(String text, LineKind kind) {
			this(text, kind, styleFor(kind));
		}
	}

	/** Style per line kind: headers and the notice are bold, items are not. */
	public static LineStyle styleFor(LineKind kind) {
		return switch (kind) {
			case PASSIVE_HEADER, ABILITY_HEADER -> new LineStyle(HEADER_COLOR, true);
			case PASSIVE -> new LineStyle(PASSIVE_COLOR, false);
			case ABILITY -> new LineStyle(ABILITY_COLOR, false);
			case NOTICE -> new LineStyle(NOTICE_COLOR, true);
		};
	}

	/** Picker subtitle under the title. */
	public static String subtitle() {
		return "Your choice shapes your ability";
	}

	/** Tooltip for the select button: concise passive/active summary. */
	public static String tooltipFor(ClassDefinition def) {
		if (def.locked()) {
			return "Coming soon";
		}
		StringBuilder sb = new StringBuilder();
		for (ConditionalPassive p : def.passives()) {
			sb.append("Passive: ").append(p.name()).append(" \u2014 ").append(p.fullDesc()).append(" ");
		}
		for (Map.Entry<String, List<HpScaledPassive>> entry : groupedHp(def).entrySet()) {
			sb.append("Passive: ").append(entry.getKey()).append(" \u2014 ");
			for (HpScaledPassive hp : entry.getValue()) {
				sb.append(hp.fullDesc()).append(" ");
			}
		}
		for (ClassDefinition.ActiveAbility a : def.actives()) {
			sb.append("Active: ").append(a.name()).append(" \u2014 ").append(a.desc()).append(" ");
		}
		sb.append("Click to choose ").append(def.displayName()).append(".");
		return sb.toString();
	}

	/** Lines shown in the card body: Passives header + one line per passive (one per HP-scaled family), then Active header + one line per active. */
	public static List<CardLine> cardLinesFor(ClassDefinition def) {
		List<CardLine> lines = new ArrayList<>();
		if (def.locked()) {
			lines.add(new CardLine("Passives: ???", LineKind.PASSIVE));
			lines.add(new CardLine("Active: ???", LineKind.ABILITY));
			lines.add(new CardLine("COMING SOON", LineKind.NOTICE));
			return lines;
		}
		lines.add(new CardLine("Passives", LineKind.PASSIVE_HEADER));
		if (!def.passives().isEmpty() || !def.hpPassives().isEmpty()) {
			for (ConditionalPassive p : def.passives()) {
				lines.add(new CardLine(p.name(), LineKind.PASSIVE));
			}
			for (String group : groupedHp(def).keySet()) {
				lines.add(new CardLine(group, LineKind.PASSIVE));
			}
		} else {
			lines.add(new CardLine("No passives yet", LineKind.PASSIVE));
		}
		if (!def.actives().isEmpty()) {
			lines.add(new CardLine("Active", LineKind.ABILITY_HEADER));
			for (ClassDefinition.ActiveAbility a : def.actives()) {
				lines.add(new CardLine(a.name(), LineKind.ABILITY));
			}
		}
		return lines;
	}

	/** HP-scaled passives read as one entry per family, in first-appearance order. */
	private static Map<String, List<HpScaledPassive>> groupedHp(ClassDefinition def) {
		Map<String, List<HpScaledPassive>> groups = new LinkedHashMap<>();
		for (HpScaledPassive hp : def.hpPassives()) {
			groups.computeIfAbsent(hp.group(), k -> new ArrayList<>()).add(hp);
		}
		return groups;
	}

	/** Accent color for card top strip (grey for locked). */
	public static int accentFor(ClassDefinition def) {
		return def.locked() ? 0xFF555555 : def.accentColor();
	}

	/** Button label and active state. */
	public static ButtonData buttonFor(ClassDefinition def) {
		return new ButtonData(def.locked() ? "Locked" : "Select", !def.locked());
	}

	public record ButtonData(String label, boolean active) {
	}

	/**
	 * Hover-independent card text: lines, tooltip, accent, button. Cached per definition.
	 */
	public record CardText(String displayName, String tagline, List<CardLine> lines, int accent, ButtonData button, String tooltip) {
	}

	/** Hover-dependent style only: border, wash, veil, name colour. */
	public record CardStyle(int borderColor, Integer washColor, Integer veilColor, int nameColor) {
	}

	/**
	 * Whole card: hover-independent text plus per-frame style.
	 * The existing entries delegate here conceptually; they stay as the seams tests pin.
	 */
	public record CardModel(CardText text, CardStyle style) {
	}

	/** Resolves the hover-independent text once per definition. */
	public static CardText cardTextFor(ClassDefinition def) {
		return new CardText(
				def.displayName(),
				def.tagline(),
				List.copyOf(cardLinesFor(def)),
				accentFor(def),
				buttonFor(def),
				tooltipFor(def));
	}

	public static CardStyle cardStyleFor(ClassDefinition def, boolean hovered) {
		if (def.locked()) {
			return new CardStyle(BORDER_COLOR, null, LOCKED_VEIL_COLOR, NAME_LOCKED_COLOR);
		}
		return new CardStyle(hovered ? HOVER_BORDER_COLOR : BORDER_COLOR,
				hovered ? HOVER_WASH_COLOR : null, null, NAME_COLOR);
	}

	/** Whole card in one decision: text + style. */
	public static CardModel cardFor(ClassDefinition def, boolean hovered) {
		return new CardModel(cardTextFor(def), cardStyleFor(def, hovered));
	}
}
