package com.hunter1712.infusedclasses;

/**
 * The focused card's colours, derived from the class accent in one pure decision:
 * the body, divider, tagline, class name, every item name, checkpoint rows and the
 * border/hover pair all sit somewhere between the accent and the card's dark base.
 * Neutral tones (section headers, descriptions) stay accent-free on purpose, and
 * nothing here reads a line's text. Pure ints, no game types.
 */
public final class CardPalette {
	/** Near-black the card body is built on. */
	private static final int BODY_BASE = 0xFF141414;
	/** Card body opacity over the backdrop. */
	private static final int BODY_ALPHA = 0xE0;
	/** Dark the plain border blends from. */
	private static final int BORDER_BASE = 0xFF2E2E2E;
	/** Warm mood tone the tagline keeps, nudged toward the accent. */
	private static final int TAGLINE_BASE = 0xFFB0A67E;
	/** Class name and every item name never draw dimmer than this relative luminance. */
	private static final double TEXT_LUMINANCE = 0.55;
	/** The one tone every description draws, passive and active alike: dim enough that every accent name stays brightest. */
	private static final int DESC_TEXT = 0xFF8C8C8C;
	/** How far each tone sits toward the accent: a whisper for the body, more for the rules. */
	private static final double BODY_TINT = 0.06;
	private static final double BORDER_TINT = 0.20;
	private static final double TAGLINE_TINT = 0.12;
	private static final double DIVIDER_TINT = 0.45;
	/** Checkpoint rows sit this far below the legible accent. */
	private static final double TIER_DIM = 0.78;

	private CardPalette() {
	}

	/** Card body: near-black carrying a whisper of the class accent. */
	public static int body(int accent) {
		return BODY_ALPHA << 24 | rgb(blend(BODY_BASE, accent, BODY_TINT));
	}

	/** The rule under the header, in the accent's own colour. */
	public static int divider(int accent) {
		return 0xFF000000 | rgb(blend(BODY_BASE, accent, DIVIDER_TINT));
	}

	/** Class name and ability names: the accent, lightened until it reads on the body. */
	public static int accentText(int accent) {
		return lightenTo(accent, TEXT_LUMINANCE);
	}

	/** Checkpoint rows: the bar's colour a step back, still tied to its bar. */
	public static int tierText(int accent) {
		return dim(accentText(accent), TIER_DIM);
	}

	/** Tagline: the warm mood tone, nudged toward the class accent. */
	public static int tagline(int accent) {
		return 0xFF000000 | rgb(blend(TAGLINE_BASE, accentText(accent), TAGLINE_TINT));
	}

	/** Plain border: dark, with a hint of the accent. */
	public static int borderPlain(int accent) {
		return 0xFF000000 | rgb(blend(BORDER_BASE, accent, BORDER_TINT));
	}

	/** Hover border: the accent itself, legible on the body. */
	public static int borderHover(int accent) {
		return accentText(accent);
	}

	/**
	 * The shared description tone: names keep their kind's colour, every description —
	 * passive, checkpoint or ability — draws this one subtle neutral.
	 */
	public static int descText() {
		return DESC_TEXT;
	}

	/** Relative luminance of an argb colour, 0 (black) to 1 (white). */
	static double luminance(int argb) {
		double r = (argb >> 16 & 0xFF) / 255.0;
		double g = (argb >> 8 & 0xFF) / 255.0;
		double b = (argb & 0xFF) / 255.0;
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	/** Blends a colour's rgb toward another's by t, keeping its own alpha. */
	static int blend(int from, int to, double t) {
		int r = mix(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
		int g = mix(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
		int b = mix(from & 0xFF, to & 0xFF, t);
		return from & 0xFF000000 | r << 16 | g << 8 | b;
	}

	/** Blends toward white by just enough to reach a luminance floor (plus rounding margin). */
	static int lightenTo(int argb, double min) {
		double lum = luminance(argb);
		if (lum >= min) {
			return argb;
		}
		// The 0.01 margin covers channel rounding, which can land a weight below the
		// floor even when the continuous blend would reach it.
		double t = Math.min(1.0, (min - lum) / (1 - lum) + 0.01);
		return blend(argb, 0xFFFFFFFF, t);
	}

	/** Scales a colour's rgb toward black, keeping its alpha. */
	static int dim(int argb, double factor) {
		int r = (int) Math.round((argb >> 16 & 0xFF) * factor);
		int g = (int) Math.round((argb >> 8 & 0xFF) * factor);
		int b = (int) Math.round((argb & 0xFF) * factor);
		return argb & 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static int rgb(int argb) {
		return argb & 0xFFFFFF;
	}

	private static int mix(int from, int to, double t) {
		return (int) Math.round(from + (to - from) * t);
	}
}
