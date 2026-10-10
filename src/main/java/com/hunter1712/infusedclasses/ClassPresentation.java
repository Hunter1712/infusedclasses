package com.hunter1712.infusedclasses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure presentation for the focused picker card. One seam for all renderers:
 * resolves the whole card (header text, detail lines with name plus description,
 * accent, button data) from the class definition in one pure decision.
 */
public final class ClassPresentation {
	private static final int HEADER_COLOR = 0xFFFFFFFF;   // white for section headers
	private static final int PASSIVE_COLOR = 0xFFBDBDBD;   // light gray for passive items
	private static final int ABILITY_COLOR = 0xFF7ED6FF;   // blue for ability items
	private static final int BORDER_COLOR = 0xFF3A3A3A;
	private static final int HOVER_BORDER_COLOR = 0xFF8A8A8A;
	private static final int HOVER_WASH_COLOR = 0x14FFFFFF;
	private static final int NAME_COLOR = 0xFFFFFFFF;

	private ClassPresentation() {
	}

	/** One rendered card line plus which section it belongs to. */
	public enum LineKind {
		PASSIVE_HEADER, PASSIVE, ABILITY_HEADER, ABILITY
	}

	/** The colour and weight a line draws with. Decided by line kind, never by its text. */
	public record LineStyle(int color, boolean bold) {
	}

	/** One rendered card line: name plus optional description, the section it belongs to, and the style it draws with. */
	public record CardLine(String name, String desc, LineKind kind, LineStyle style) {
		/** A line's style comes from its kind, so a renderer never has to read the line. */
		public CardLine(String name, String desc, LineKind kind) {
			this(name, desc, kind, styleFor(kind));
		}

		/** Full text: name alone, or name plus description. The renderer draws the parts in two tones. */
		public String text() {
			return desc == null || desc.isEmpty() ? name : name + " \u2014 " + desc;
		}
	}

	/** Style per line kind: headers are bold, items are not. Continuation lines draw with their parent item's style. */
	public static LineStyle styleFor(LineKind kind) {
		return switch (kind) {
			case PASSIVE_HEADER, ABILITY_HEADER -> new LineStyle(HEADER_COLOR, true);
			case PASSIVE -> new LineStyle(PASSIVE_COLOR, false);
			case ABILITY -> new LineStyle(ABILITY_COLOR, false);
		};
	}

	/** Picker subtitle under the title. */
	public static String subtitle() {
		return "Your choice shapes your ability";
	}

	/**
	 * Lines shown on the focused card: Passives header plus one detail line per passive
	 * (one per HP-scaled family, with joined descriptions) plus one per flip buff,
	 * then Active header plus one detail line per fired active. Every detail line carries
	 * name plus short description, so nothing hides behind hover.
	 */
	public static List<CardLine> cardLinesFor(ClassDefinition def) {
		List<CardLine> lines = new ArrayList<>();
		lines.add(new CardLine("Passives", null, LineKind.PASSIVE_HEADER));
		if (!def.passives().isEmpty() || !def.hpPassives().isEmpty() || !def.flipBuffs().isEmpty()) {
			for (ConditionalPassive p : def.passives()) {
				lines.add(new CardLine(p.name(), p.fullDesc(), LineKind.PASSIVE));
			}
			for (Map.Entry<String, List<HpScaledPassive>> entry : groupedHp(def).entrySet()) {
				StringBuilder joined = new StringBuilder();
				for (HpScaledPassive hp : entry.getValue()) {
					if (!joined.isEmpty()) {
						joined.append(" ");
					}
					joined.append(hp.fullDesc());
				}
				lines.add(new CardLine(entry.getKey(), joined.toString(), LineKind.PASSIVE));
			}
			addAbilities(lines, def.flipBuffs(), LineKind.PASSIVE);
		} else {
			lines.add(new CardLine("No passives yet", null, LineKind.PASSIVE));
		}
		if (!def.actives().isEmpty()) {
			lines.add(new CardLine("Active", null, LineKind.ABILITY_HEADER));
			addAbilities(lines, def.actives(), LineKind.ABILITY);
		}
		return lines;
	}

	private static void addAbilities(List<CardLine> lines, List<ClassDefinition.ActiveAbility> abilities, LineKind kind) {
		for (ClassDefinition.ActiveAbility ability : abilities) {
			lines.add(new CardLine(ability.name(), ability.desc(), kind));
		}
	}

	/** HP-scaled passives read as one entry per family, in first-appearance order. */
	private static Map<String, List<HpScaledPassive>> groupedHp(ClassDefinition def) {
		Map<String, List<HpScaledPassive>> groups = new LinkedHashMap<>();
		for (HpScaledPassive hp : def.hpPassives()) {
			groups.computeIfAbsent(hp.group(), k -> new ArrayList<>()).add(hp);
		}
		return groups;
	}

	/** Accent color for the card top strip: the class's own accent. */
	public static int accentFor(ClassDefinition def) {
		return def.accentColor();
	}

	/** Button label and active state: every visible page is playable. */
	public static ButtonData buttonFor(ClassDefinition def) {
		return new ButtonData("Select", true);
	}

	public record ButtonData(String label, boolean active) {
	}

	/**
	 * Hover-independent card text: lines, accent, button. Resolved through
	 * {@link #cardFor} by every caller; kept public only as the seam tests pin.
	 */
	public record CardText(String displayName, String tagline, List<CardLine> lines, int accent, ButtonData button) {
	}

	/** Hover-dependent style only: border, wash, name colour. */
	public record CardStyle(int borderColor, Integer washColor, int nameColor) {
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
				buttonFor(def));
	}

	public static CardStyle cardStyleFor(ClassDefinition def, boolean hovered) {
		return new CardStyle(hovered ? HOVER_BORDER_COLOR : BORDER_COLOR,
				hovered ? HOVER_WASH_COLOR : null, NAME_COLOR);
	}

	/** Whole card in one decision: text + style. */
	public static CardModel cardFor(ClassDefinition def, boolean hovered) {
		return new CardModel(cardTextFor(def), cardStyleFor(def, hovered));
	}
}
