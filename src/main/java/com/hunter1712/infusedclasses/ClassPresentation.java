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

	private ClassPresentation() {
	}

	/** One rendered card line plus which section it belongs to. */
	public enum LineKind {
		PASSIVE_HEADER, PASSIVE, TIER, ABILITY_HEADER, ABILITY
	}

	/** The colour and weight a line draws with. Decided by line kind, never by its text. */
	public record LineStyle(int color, boolean bold) {
	}

	/** One rendered card line: name plus optional description, the section it belongs to, and the style it draws with. */
	public record CardLine(String name, String desc, LineKind kind, LineStyle style) {
		/** A line's style comes from its kind and the class accent, so a renderer never has to read the line. */
		public CardLine(String name, String desc, LineKind kind, int accent) {
			this(name, desc, kind, styleFor(kind, accent));
		}

		/** Full text: name alone, or name plus description. The renderer draws the parts in two tones. */
		public String text() {
			return desc == null || desc.isEmpty() ? name : name + " \u2014 " + desc;
		}

		/**
		 * Where the description starts inside {@link #text()}: derived from the join, so
		 * the tone boundary cannot drift from the text. A line without a description
		 * has its boundary at the end, leaving the whole row the name.
		 */
		public int descStart() {
			return desc == null || desc.isEmpty() ? text().length() : text().length() - desc.length();
		}
	}

	/**
	 * Style per line kind and class accent: headers white bold, every item name the
	 * accent lightened until it reads, checkpoint rows the same accent dimmed. Text
	 * never decides.
	 */
	public static LineStyle styleFor(LineKind kind, int accent) {
		return switch (kind) {
			case PASSIVE_HEADER, ABILITY_HEADER -> new LineStyle(HEADER_COLOR, true);
			case PASSIVE, ABILITY -> new LineStyle(CardPalette.accentText(accent), false);
			case TIER -> new LineStyle(CardPalette.tierText(accent), false);
		};
	}

	/** Picker subtitle under the title. */
	public static String subtitle() {
		return "Your choice shapes your ability";
	}

	/**
	 * Lines shown on the focused card: Passives header plus, per passive, its rules line
	 * then one `TIER` row per checkpoint (e.g. `Steady at 15`, drawn indented so it hangs
	 * off its bar), one per HP-scaled family (with joined descriptions), one per always-on
	 * row, one per flip buff plus the generated Crooked Dice and Streak lines for a class
	 * carrying dice, then Actives header plus one detail line per fired active. Every detail
	 * line carries name plus short description, so nothing hides behind hover.
	 */
	public static List<CardLine> cardLinesFor(ClassDefinition def) {
		int accent = def.accentColor();
		List<CardLine> lines = new ArrayList<>();
		lines.add(new CardLine("Passives", null, LineKind.PASSIVE_HEADER, accent));
		if (!def.passives().isEmpty() || !def.hpPassives().isEmpty() || !def.alwaysOn().isEmpty()
				|| !def.flipBuffs().isEmpty() || def.critRule().hasDice()) {
			for (ConditionalPassive p : def.passives()) {
				lines.add(new CardLine(p.name(), p.rulesDesc(), LineKind.PASSIVE, accent));
				// Each checkpoint gets its own TIER row: a bar scans tier by tier
				// instead of running together as one paragraph.
				for (ConditionalPassive.Tier tier : p.tiers()) {
					lines.add(new CardLine(tier.name() + " at " + tier.threshold(),
							p.tierDesc(tier), LineKind.TIER, accent));
				}
			}
			for (Map.Entry<String, List<HpScaledPassive>> entry : groupedHp(def).entrySet()) {
				StringBuilder joined = new StringBuilder();
				for (HpScaledPassive hp : entry.getValue()) {
					if (!joined.isEmpty()) {
						joined.append(" ");
					}
					joined.append(hp.fullDesc());
				}
				lines.add(new CardLine(entry.getKey(), joined.toString(), LineKind.PASSIVE, accent));
			}
			for (ClassDefinition.AlwaysOn alwaysOn : def.alwaysOn()) {
				lines.add(new CardLine(alwaysOn.name(), alwaysOn.desc(), LineKind.PASSIVE, accent));
			}
			for (ClassDefinition.FlipBuff flip : def.flipBuffs()) {
				lines.add(new CardLine(flip.name(), flip.desc(), LineKind.PASSIVE, accent));
			}
			if (def.critRule().hasDice()) {
				lines.add(new CardLine(CritRule.CROOKED_DICE, def.critRule().crookedDiceDesc(), LineKind.PASSIVE, accent));
				lines.add(new CardLine(CritRule.STREAK, def.critRule().streakDesc(), LineKind.PASSIVE, accent));
			}
		} else {
			lines.add(new CardLine("No passives yet", null, LineKind.PASSIVE, accent));
		}
		if (!def.actives().isEmpty()) {
			lines.add(new CardLine("Actives", null, LineKind.ABILITY_HEADER, accent));
			for (ClassDefinition.ActiveAbility active : def.actives()) {
				lines.add(new CardLine(active.name(), active.desc(), LineKind.ABILITY, accent));
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

	/**
	 * Hover-independent card text: lines, accent. An internal seam of
	 * {@link #cardFor}; callers and tests cross the whole card instead.
	 */
	public record CardText(String displayName, String tagline, List<CardLine> lines, int accent) {
	}

	/** Hover-dependent style only: border and name colour, both derived from the class accent. */
	public record CardStyle(int borderColor, int nameColor) {
	}

	/**
	 * Whole card: hover-independent text plus per-frame style.
	 * The existing entries delegate here conceptually; they stay as the seams tests pin.
	 */
	public record CardModel(CardText text, CardStyle style) {
	}

	/** Resolves the hover-independent text once per definition. */
	private static CardText cardTextFor(ClassDefinition def) {
		return new CardText(
				def.displayName(),
				def.tagline(),
				List.copyOf(cardLinesFor(def)),
				def.accentColor());
	}

	private static CardStyle cardStyleFor(int accent, boolean hovered) {
		return new CardStyle(hovered ? CardPalette.borderHover(accent) : CardPalette.borderPlain(accent),
				CardPalette.accentText(accent));
	}

	/** Whole card in one decision: text + style. */
	public static CardModel cardFor(ClassDefinition def, boolean hovered) {
		return new CardModel(cardTextFor(def), cardStyleFor(def.accentColor(), hovered));
	}
}
